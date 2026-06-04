# Migration Plan — Fat Agent + LLM-Proxy Server

> **Supersedes the earlier draft of this file**, which planned a thin control
> plane + fat worker that owned the LLM. The model was inverted; this is the
> current plan. See `DISTRIBUTED_ARCHITECTURE.md` and `AGENT_AND_LICENSING.md`.

The numbered steps to move from the single-process (`hosted`) runtime to the
target topology:

- **Server** → LLM proxy + licensing + config distribution + system-of-record
  + live-stream relay (`app.runtime.mode=proxy`).
- **Agent** → a new **Go** binary that owns the turn loop, prompt assembly,
  tools, MCP, and skills, and calls the server proxy for model access.
- **CLI (Picocli) + Web** → dummy clients that drive sessions and render the
  relayed stream, and run the install/license flow.

**Guiding rules**

- `hosted` mode keeps working at every step (default, gated behind
  `app.runtime.mode`).
- The Go agent must preserve the behavior contract in `PARITY_VERIFICATION.md`.
- Provider keys never leave the server; the agent calls the proxy only.
- Live stream relays through the server — no P2P/WebRTC/TURN.

Each step: **goal · key changes · done-when**. Phases are independently shippable.

---

## Phase A — Server becomes the proxy + control plane

### Step 1 — Add the `proxy` runtime mode
- **Goal:** select the thin server without forking code.
- **Changes:** extend `app.runtime.mode` to `hosted | proxy`
  (`application.yml:50`); gate bean groups in the config layer with
  `@ConditionalOnProperty`. `hosted` keeps every bean (default).
- **Done when:** all existing tests pass in `hosted`; the server boots in
  `proxy` with execution beans absent.

### Step 2 — LLM proxy endpoint + service
- **Goal:** serve model completions to agents, with keys server-side.
- **Changes:** a proxy service wrapping `runtime.provider`
  (`ModelProvider.stream/complete`, `DefaultModelProviderRegistry`,
  `SpringAiModelProvider`, `ProviderModels`). Accept an assembled prompt + model
  id, route to a provider, stream back. **Server never assembles prompts.**
- **Done when:** a test client sends a `PromptSection` list + model and receives
  a streamed completion through the proxy against the stub provider.

### Step 3 — Licensing service (identity + entitlements)
- **Goal:** issue and enforce licenses.
- **Changes:** `server.license` — issue signed tokens (agentId/customerId,
  seats, expiry, tier, optional token budget); validate at connect; re-check +
  meter at each proxy call. Flyway table `licenses`; usage metering hook in the
  proxy (Step 2).
- **Done when:** an expired/over-budget token is rejected at the proxy; valid
  usage is metered.

### Step 4 — Agent registry + WS endpoint (agents dial in)
- **Goal:** accept agent connections and track them.
- **Changes:** new agent WS endpoint (distinct from the client WS);
  `server.agents` registry fed by `REGISTER`/`HEARTBEAT`; license check on the
  handshake (Step 3). Flyway tables `session_bindings`, `agent_audit`.
- **Done when:** multiple agents register concurrently and are listed via
  `GET /api/v1/agents`.

### Step 5 — Config distribution service
- **Goal:** push capability profiles to agents.
- **Changes:** `server.config` — version-stamped capability profiles; serve
  `CONFIG_PULL`; push `CONFIG_UPDATE`; `POST /agents/{id}/provision` records to
  `agent_audit`.
- **Done when:** provisioning a skill/MCP on agent A pushes a `CONFIG_UPDATE`
  only to A and is audited.

### Step 6 — System-of-record + live relay
- **Goal:** persist synced sessions and fan live events to clients.
- **Changes:** ingest `SESSION_SYNC` deltas → persist via `JpaSessionManager`;
  relay the same events to client-WS subscribers (reuse
  `StreamingWebSocketHandler` + `EventSerializer`). `SESSION_INPUT` forwards a
  client's input down to the bound agent.
- **Done when:** a synced transcript matches the agent's, a cold
  `GET /sessions/{id}` returns full history, and a client sees the live stream.

---

## Phase B — The Go agent (the runtime)

### Step 7 — Scaffold the agent + link
- **Goal:** a Go binary that connects and registers.
- **Changes:** new `apps/agent` (Go); outbound WS client (e.g. `gorilla` /
  `nhooyr`); `REGISTER` with license + baked-in inventory; `HEARTBEAT`;
  auto-reconnect with backoff.
- **Done when:** a started agent appears in the server registry and heartbeats.

### Step 8 — Port the turn loop
- **Goal:** drive a turn from the agent against the proxy.
- **Changes:** port `DefaultTurnEngine` semantics to Go — prompt → `LLM_REQUEST`
  → parse → run tools → loop (max 10); emit the same `RuntimeEvent` types; ship
  them via `SESSION_SYNC`.
- **Done when:** a turn driven from the agent produces the event sequence a
  `hosted` turn produces (parity check).

### Step 9 — Port prompt assembly
- **Goal:** assemble prompts on the agent.
- **Changes:** port `runtime.prompt` — 5-level precedence, 12-surface catalog,
  section caching — and add the **local environment block** (cwd/OS/git/files).
  Consume skill content + capability profile from `CONFIG_UPDATE`.
- **Done when:** assembled prompts match the `hosted` assembler for equivalent
  inputs (precedence + caching parity).

### Step 10 — Port local tools
- **Goal:** run tools on the agent machine.
- **Changes:** port `runtime.tools` builtin file/shell/git/glob/grep + the
  deny→mode→permission pipeline + alphabetical ordering.
- **Done when:** tool ordering + filtering + execution match `hosted`; bash/file
  side-effects land on the agent's filesystem.

### Step 11 — MCP hosting + skills
- **Goal:** per-agent MCP + skills.
- **Changes:** host MCP servers locally; load skills (PROJECT→USER→MANAGED) from
  distributed content; apply `CONFIG_UPDATE` installs (launch MCP, write skills).
- **Done when:** an MCP/skill provisioned to the agent is usable in its turns.

### Step 12 — Local session state + sync up
- **Goal:** own the working dir; keep the server record current.
- **Changes:** hold the live transcript + working directory; stream
  `SESSION_SYNC` deltas; pin a session to this agent for its lifetime.
- **Done when:** turn 2's bash sees turn 1's file, and the server record matches.

---

## Phase C — Make the clients dummy

### Step 13 — CLI install/license + drive
- **Goal:** Picocli CLI drives sessions and provisions agents.
- **Changes:** add commands to request a licensed agent + install token, list
  agents, provision skill/MCP, submit input, and render the relayed stream.
  Remove any client-side runtime logic.
- **Done when:** the CLI works end-to-end against a `proxy` server + a Go agent,
  holding no execution logic.

### Step 14 — Web install/license + drive
- **Goal:** the same for the Web UI.
- **Changes:** install/license UI; agent roster + provisioning; history from the
  server; live via the relayed WS.
- **Done when:** the web client mirrors the CLI capabilities with no runtime
  logic.

---

## Phase D — Operations & rollout

### Step 15 — Metering, billing & entitlement tiers
- **Goal:** turn metering into product surfaces.
- **Changes:** usage aggregation per agent/customer; tier definitions; budget
  caps enforced at the proxy; reporting endpoints.
- **Done when:** usage is queryable per customer and caps are enforced.

### Step 16 — Observability across the proxy hop
- **Goal:** trace work across machines.
- **Changes:** per-agent metrics (active sessions, turn latency, tokens);
  propagate trace context from `SESSION_INPUT`/`LLM_REQUEST` through the agent's
  turn so a trace spans client → server → agent → proxy.
- **Done when:** a single turn is traceable end-to-end with per-agent dashboards.

### Step 17 — Agent packaging & licensed distribution
- **Goal:** "create as many agent machines as needed."
- **Changes:** Go cross-compile (Linux/macOS/Windows); the install token/script
  flow; optional CLI+agent bundle; `task agent:build`, `task proxy:run`;
  document scaling and labeling agents by capability.
- **Done when:** N licensed agents can be installed from one artifact and point
  at one server.

### Step 18 — Parity gate & cutover
- **Goal:** prove the Go agent preserves behavior, then default to the split.
- **Changes:** run the `PARITY_VERIFICATION.md` checklist against the Go agent;
  an integration test that runs a `proxy` server + ≥1 Go agent and asserts
  turn/event/transcript parity with `hosted`. Document the rollout: `hosted` →
  split behind the mode flag → split as production default.
- **Done when:** parity tests pass and the rollout runbook is written.

---

## Suggested Sequencing

| Milestone | Steps | Outcome |
|-----------|-------|---------|
| **M1 — Proxy server** | 1–6 | Server serves model calls, issues/enforces licenses, distributes config, records + relays. `hosted` untouched. |
| **M2 — Walking agent** | 7–12 | A Go agent runs a full turn locally, calling the proxy, syncing the record up. |
| **M3 — Dummy clients** | 13–14 | CLI + Web drive sessions and install agents; no runtime logic in clients. |
| **M4 — Productized** | 15–18 | Metering/billing, observability, licensed packaging, parity-gated cutover. |

## Reuse Notes

- Every server step is gated by `app.runtime.mode`, so `proxy` can ship dark and
  flip per environment.
- The proxy reuses `runtime.provider` wholesale — Spring AI stays exactly behind
  the provider boundary, as the original project principle intends.
- The Go agent ports `runtime.query`, `runtime.prompt`, `runtime.tools`,
  `runtime.skills` — `PARITY_VERIFICATION.md` is the behavior contract for the
  port, not a rewrite license.
- The protocol (`DISTRIBUTED_ARCHITECTURE.md`) is the stable contract; new
  message/event types extend it without reworking transport.
- One licensed agent binary (Step 17) is the unit of scale: install more
  machines, label them, provision capabilities per agent.
