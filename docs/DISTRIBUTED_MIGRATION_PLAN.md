# Distributed Migration Plan — Control Plane + Agent Workers

The numbered, reusable steps to move from the current single-process (`hosted`)
runtime to a **control plane + distributed agent workers** topology, as
specified in `DISTRIBUTED_ARCHITECTURE.md`.

**Guiding rules**

- `hosted` mode must keep working at every step (default, zero behavior drift).
  The split is gated behind `app.runtime.mode`.
- No new message broker — control plane ↔ worker is WebSocket (worker dials out).
- Workers own execution + LLM keys; the control plane is API + orchestration only.
- Preserve the parity contract in `PARITY_VERIFICATION.md` — turn semantics,
  prompt precedence, tool ordering, and event types do not change.

Each step lists **goal · key changes · done-when**. Steps are grouped into
phases; phases are independently shippable.

---

## Phase A — Protocol & Mode Scaffolding (no behavior change)

### Step 1 — Define the worker protocol contracts
- **Goal:** a typed, versioned message set for the worker WS channel.
- **Changes:** add a `protocol` package with the envelope
  `{ type, correlationId, workerId, payload }` and the message types from the
  spec (`REGISTER`, `HEARTBEAT`, `EVENT`, `TRANSCRIPT_APPEND`, `TURN_RESULT`,
  `ASK_USER`, `PROVISION_RESULT`, `DISPATCH_TURN`, `RESUME_SESSION`,
  `PROVISION`, `CANCEL`, `PING`). Reuse `RuntimeEvent` + `EventSerializer` for
  the `EVENT` payload and the existing `TurnRequest`/`TranscriptEntry` records.
- **Done when:** contracts compile and round-trip in a serialization unit test.

### Step 2 — Define orchestration domain types
- **Goal:** the data the control plane needs to route and audit.
- **Changes:** `WorkerDescriptor` (id, labels, capabilities, inventory),
  `WorkerInventory` (skills, MCP servers, providers, tool names),
  `SessionBinding` (sessionId, workerId, boundAt, lastActive),
  `WorkerAuditRecord`. Add Flyway migrations for `session_bindings` and
  `worker_audit`.
- **Done when:** migrations apply on H2 and Postgres; entities validate
  (`ddl-auto: validate`).

### Step 3 — Make bean wiring mode-aware
- **Goal:** select component sets by `app.runtime.mode` without forking code.
- **Changes:** extend the mode to `hosted | control-plane | worker`. Annotate
  `RuntimeConfig` bean groups with `@ConditionalOnProperty(app.runtime.mode)`.
  `hosted` keeps every bean (default).
- **Done when:** all 705 existing tests pass with `mode=hosted`; the app boots
  in each mode with placeholder beans.

---

## Phase B — The Worker App

### Step 4 — Stand up the worker runtime
- **Goal:** an LLM-agnostic Spring Boot process that owns execution.
- **Changes:** under `mode=worker`, load `runtime.query` (`DefaultTurnEngine`),
  `runtime.tools` (+ builtin file/shell/git), `runtime.skills`, MCP bridge,
  `runtime.provider` (this worker's keys), `runtime.permissions`,
  `runtime.planning`, `runtime.agents`. No client REST API, no DB ownership.
- **Done when:** a worker boots, resolves its tools, and runs a turn fully
  in-process against a stub provider.

### Step 5 — Worker → control-plane link (register + heartbeat)
- **Goal:** the worker joins the pool over an outbound WS.
- **Changes:** `worker.link` WS client that dials the control plane, sends
  `REGISTER` with the live `WorkerInventory`, then periodic `HEARTBEAT` (load,
  active sessions, health). Auto-reconnect with backoff.
- **Done when:** a started worker appears in the control-plane registry and
  heartbeats; disconnect is detected.

### Step 6 — Worker dispatch handler
- **Goal:** run a dispatched turn and stream results back.
- **Changes:** on `DISPATCH_TURN`, build the `TurnRequest`, call
  `executeTurn()`, and ship each `RuntimeEvent` as an `EVENT` frame plus
  `TRANSCRIPT_APPEND` per entry, ending with `TURN_RESULT`. Forward
  `AskUserRequestedEvent` as `ASK_USER`. Honor `CANCEL`.
- **Done when:** a turn dispatched over WS produces the same event stream a
  `hosted` turn produces (parity check against `PARITY_VERIFICATION.md`).

---

## Phase C — The Control Plane

### Step 7 — Worker WS endpoint + registry
- **Goal:** accept worker connections and track them.
- **Changes:** new WS endpoint (e.g. `/ws/worker`) distinct from the client WS;
  `controlplane.workers` registry (in-memory liveness + persisted descriptors)
  fed by `REGISTER`/`HEARTBEAT`. Reuse the existing auth filter on the handshake.
- **Done when:** multiple workers register concurrently and are listed.

### Step 8 — Scheduler with sticky binding
- **Goal:** pick a worker per session and pin it.
- **Changes:** `controlplane.scheduler` — on first turn for a session, choose a
  worker (capability-match → least-loaded), persist a `SessionBinding`, and
  route all later turns for that session to the same worker.
- **Done when:** repeated turns for one session always hit the same worker;
  different sessions spread across workers.

### Step 9 — Dispatch + event relay
- **Goal:** turn the blocking REST turn into a dispatched, streamed turn.
- **Changes:** `SessionController` turn endpoint → resolve binding → send
  `DISPATCH_TURN`. `controlplane.relay` receives the worker's `EVENT` frames and
  fans them out to the **client** WS subscribers of that session (reusing
  `StreamingWebSocketHandler`). Return `202 + turnId`; retain a synchronous
  compat path that awaits `TURN_RESULT` and returns the final output.
- **Done when:** a CLI/web client submits a turn and renders the live event
  stream end-to-end through the control plane.

### Step 10 — Persist transcript deltas centrally
- **Goal:** DB is the source of truth for session state.
- **Changes:** the relay persists each `TRANSCRIPT_APPEND` via `JpaSessionManager`;
  `TURN_RESULT` finalizes the turn. The control plane never reconstructs prompts
  — it only stores.
- **Done when:** after a turn, the DB transcript matches the worker's, and a
  cold `GET /sessions/{id}` returns full history.

---

## Phase D — Stickiness & Resilience

### Step 11 — Failover + session resume
- **Goal:** survive worker loss without losing sessions.
- **Changes:** heartbeat-timeout marks a worker dead and unbinds its sessions.
  The next turn rebinds to a healthy worker and sends `RESUME_SESSION` with the
  transcript replayed from the DB to rebuild warm context.
- **Done when:** killing the bound worker mid-session, then sending another
  turn, transparently continues on a new worker with full history.

### Step 12 — Backpressure & cancellation
- **Goal:** behave under no-capacity and cancel.
- **Changes:** queue dispatches when no worker matches; surface a pending signal
  to the client; dispatch when capacity appears. Wire `CANCEL` from a client
  stop action through to the worker.
- **Done when:** turns queue and later run when a worker joins; a cancel stops
  an in-flight turn.

---

## Phase E — Per-Agent Skills, MCP & Audit

### Step 13 — Inventory advertisement + worker roster API
- **Goal:** clients can see workers and what each can do.
- **Changes:** workers advertise `WorkerInventory` on `REGISTER` and on change;
  add `GET /api/v1/workers` and `GET /api/v1/workers/{id}`.
- **Done when:** the roster reflects each worker's skills/MCP/providers/tools
  and updates live.

### Step 14 — Provisioning API (install skill/MCP on a specific worker)
- **Goal:** the CLI installs a skill or MCP onto a chosen agent.
- **Changes:** `POST /api/v1/workers/{id}/provision { type, ref, args }` →
  control plane sends `PROVISION` → worker installs (writes to its skills dir /
  registers the MCP server) → `PROVISION_RESULT` → updated inventory re-advertised.
- **Done when:** provisioning a skill/MCP on worker A makes it usable there and
  not on worker B.

### Step 15 — Audit trail
- **Goal:** every inventory change is traceable.
- **Changes:** write each provision to `worker_audit(worker_id, actor, type,
  ref, result, at)`; add a read endpoint.
- **Done when:** the audit log shows who installed what, where, and when.

---

## Phase F — Make the Clients Dummy

### Step 16 — Strip and re-point CLI + Web UI
- **Goal:** clients submit + render only; no business logic.
- **Changes:** CLI/web submit turns via REST, stream via the client WS, list
  workers, target a worker, and request provisioning. Remove any client-side
  prompt/transcript/tool logic. Confirm clients hold no execution state.
- **Done when:** CLI and web work identically against `control-plane` mode with
  zero runtime logic in the client.

---

## Phase G — Operations & Rollout

### Step 17 — Worker authentication & identity
- **Goal:** only trusted workers join.
- **Changes:** authenticate workers on the WS handshake (shared secret via the
  existing `app.auth` keys for v1; mTLS / per-worker identity for production).
- **Done when:** an unauthenticated worker is rejected; authenticated workers
  carry a stable identity in the registry and audit.

### Step 18 — Observability across the WS hop
- **Goal:** see work across machines.
- **Changes:** per-worker metrics (active sessions, turn latency, tool counts);
  propagate the existing trace context from control plane through `DISPATCH_TURN`
  into the worker's turn so a trace spans both processes.
- **Done when:** a single turn is traceable control-plane → worker → back, and
  per-worker dashboards exist.

### Step 19 — Packaging & scaling
- **Goal:** "create as many agent machines as we need."
- **Changes:** worker jlink/Docker image; Taskfile targets `task worker:run`
  and `task control-plane:run`; document scaling N workers and labeling them by
  capability.
- **Done when:** N workers can be launched from a single image and pointed at
  one control plane via config.

### Step 20 — Parity gate & cutover
- **Goal:** prove the split preserves behavior, then default to it.
- **Changes:** run the `PARITY_VERIFICATION.md` checklist in split mode; add an
  integration test that runs `control-plane` + ≥2 `worker` processes and asserts
  turn/event/transcript parity with `hosted`. Document the rollout: `hosted` →
  split behind the mode flag → split as production default.
- **Done when:** split-mode parity tests pass and the rollout runbook is written.

---

## Suggested Sequencing

| Milestone | Steps | Outcome |
|-----------|-------|---------|
| **M1 — Plumbing** | 1–3 | Contracts + modes exist; `hosted` untouched. |
| **M2 — One remote turn** | 4–10 | A turn runs on a separate worker, streamed through the control plane, persisted centrally. |
| **M3 — Production-shaped** | 11–12 | Sticky sessions survive failover; backpressure + cancel. |
| **M4 — Per-agent capability** | 13–15 | Per-worker skills/MCP, installable + audited. |
| **M5 — Dummy clients** | 16 | CLI/web carry no logic. |
| **M6 — Ops & cutover** | 17–20 | Auth, observability, scaling, parity-gated rollout. |

## Reuse Notes

This plan is intended to be run repeatedly / incrementally:

- Every step is gated by `app.runtime.mode`, so the split can ship dark and be
  enabled per environment.
- The worker protocol (Step 1) is the stable contract — new event/message types
  extend it without reworking transport.
- The same worker image (Step 19) is the unit of scale: add machines, label
  them, and the scheduler (Step 8) routes by capability.
