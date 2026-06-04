# Distributed Architecture — Control Plane + Agent Workers

## Purpose

Today the runtime is a single Spring Boot process: the same JVM hosts the
client API, the turn engine, the tools, the LLM providers, and the session
store. This document specifies the target architecture where:

- **The server becomes a thin control plane** — an API + orchestration layer.
  It holds no LLM keys, runs no tools, and executes no agent turns.
- **Agents run on separate machines (workers)** — each worker is an
  LLM-agnostic Spring Boot process that owns the full turn loop: prompt
  assembly, model calls, tool execution, skills, and MCP. Skills and MCP
  servers are installed and audited **per worker**.
- **The CLI and Web UI are dummy clients** — they submit commands and render
  streamed events. No business logic lives in a client.

The goal is to **create as many agent machines as we need** and route work to
them, while the control plane stays small and stateless-of-execution.

This is a specification and a migration target. It does not change behavior on
its own — see `DISTRIBUTED_MIGRATION_PLAN.md` for the numbered steps.

## Design Decisions (locked)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Control-plane ↔ worker transport | **WebSocket** (reuse existing WS stack) | Worker dials *out* to the control plane → no inbound ports on worker machines, traverses NAT/firewalls, reuses `EventSerializer` and the existing WS infra. |
| Where execution lives | **Worker owns the full turn loop** | Sandboxed bash/file/git run on the worker, not the control plane. Control plane is "just an API layer." |
| LLM provider | **Worker-side, LLM-agnostic** | Provider keys and Spring AI config live on the worker. The control plane never calls a model and holds no keys. |
| Skills & MCP | **Per-agent, audited** | Each worker advertises its own inventory. The CLI can request installing a specific skill/MCP onto a specific worker; every install is recorded. |
| Session affinity | **Sticky / stateful workers** | A session is pinned to one worker for its lifetime; the worker caches warm context. The central DB remains the source of truth for failover. |
| State ownership | **Control plane owns durable state**; worker caches | Sessions, tasks, transcripts, bindings, and audit live in the DB. Workers stream deltas back. |

## Three Runtime Modes (one codebase)

The architecture is selected by the **existing** `app.runtime.mode` property
(`application.yml:50`, today defaulting to `hosted`). We extend the enum
instead of forking the codebase:

| `app.runtime.mode` | Loads | Use |
|--------------------|-------|-----|
| `hosted` (today) | Everything in one JVM | Local dev, single-box, tests. **Unchanged.** |
| `control-plane` | API + WS + store + registry + scheduler + relay | The orchestration server. No turn engine, no tools, no providers. |
| `worker` | Turn engine + tools + skills + MCP + providers + worker WS client | An agent machine. No client API, no DB ownership. |

Bean wiring becomes mode-aware via `@ConditionalOnProperty(app.runtime.mode)`
in the config layer (`RuntimeConfig`). `hosted` keeps every bean and is the
default, so nothing breaks for existing users.

## Topology

```
        CLI  ┐                              ┌── Worker A  (skills: x,y  · MCP: github)
   Web UI  ──┤  REST + client WS            │     TurnEngine + Tools + Providers
             ▼                              │
      ┌─────────────────────┐   worker WS   ├── Worker B  (skills: z    · MCP: filesystem)
      │   CONTROL PLANE      │◀═════════════▶│     TurnEngine + Tools + Providers
      │  (app.runtime.mode=  │  (each worker │
      │     control-plane)   │   dials out)  └── Worker C  ...
      │                      │
      │  • Client API (REST) │
      │  • Client WS (stream)│        ┌──────────────┐
      │  • Worker registry   │───────▶│  Postgres/H2 │  sessions, tasks, transcripts,
      │  • Scheduler/router  │        │  (durable)   │  session_bindings, worker_audit
      │  • Event relay       │        └──────────────┘
      │  • Provisioning API  │
      │  • NO LLM keys       │
      └─────────────────────┘
```

Clients only ever talk to the control plane. Workers only ever talk to the
control plane. Clients and workers never talk directly.

## Component Responsibilities

### Control plane (`app.runtime.mode=control-plane`)

| Concern | Detail | Reuses today |
|---------|--------|--------------|
| Client API | REST controllers (`SessionController`, `TaskController`, `ConfigController`, `CostController`, `PermissionController`) | `app.api` — contracts unchanged |
| Client streaming | `StreamingWebSocketHandler` — clients subscribe to a session, receive `RuntimeEvent` JSON | `app.ws` — unchanged |
| Durable store | Sessions, tasks, transcripts via JPA | `app.persistence` (`JpaSessionManager`, `JpaTaskManager`) — unchanged |
| **Worker registry** *(new)* | Workers register over WS, advertise inventory + capabilities, heartbeat | new `controlplane.workers` |
| **Scheduler / router** *(new)* | Picks a worker per session; persists `session → worker` binding; enforces stickiness; matches by labels/capabilities/load | new `controlplane.scheduler` |
| **Event relay** *(new)* | Receives event stream from a worker, fans it out to the client-WS subscribers of that session, persists transcript deltas | new `controlplane.relay` |
| **Provisioning API** *(new)* | Forwards "install skill/MCP on worker X" to that worker; records to audit | new `controlplane.provision` |
| LLM | **None.** Holds no provider keys, never calls a model. | — |

### Worker (`app.runtime.mode=worker`)

| Concern | Detail | Reuses today |
|---------|--------|--------------|
| Turn engine | The full loop: prompt → model → parse → tools → loop (max 10), event emission, cost | `runtime.query` (`DefaultTurnEngine`) — unchanged |
| Tools | All builtin tools execute **locally on this machine** (file/shell/git/web/memory) + MCP bridge | `runtime.tools` — unchanged |
| Skills | Per-agent skill inventory (PROJECT→USER→MANAGED), installed/audited | `runtime.skills` |
| Providers | LLM-agnostic; this worker's keys/config (OpenAI, Anthropic, Ollama, Copilot, Codex) | `runtime.provider` |
| Permissions / planning | Enforced where tools run | `runtime.permissions`, `runtime.planning` |
| Sub-delegation | Coordinator→worker nesting stays in-process **within** a worker | `runtime.agents` (`DefaultWorkerOrchestrator`) — unchanged for now |
| **Worker WS client** *(new)* | Dials the control plane, REGISTERs, HEARTBEATs, receives dispatches, ships events back | new `worker.link` |
| **Sticky session cache** *(new)* | Holds warm `Transcript` for sessions bound here; streams appends to control plane | new `worker.session` |

### Clients (CLI, Web UI) — dummy

| Allowed | Not allowed |
|---------|-------------|
| Create/resume/close sessions and tasks via REST | Run any tool |
| Submit a turn (input text) via REST | Call any LLM |
| Subscribe to a session over WS and render events | Assemble prompts / hold transcripts as truth |
| List workers, request skill/MCP provisioning on a worker | Make scheduling/routing decisions |

## The Worker Protocol (over WebSocket)

A single bidirectional WS channel per worker, distinct from the client WS
endpoint. The worker is the WS client; the control plane is the WS server
(new endpoint, e.g. `/ws/worker`). All frames are JSON envelopes:

```json
{ "type": "<MESSAGE_TYPE>", "correlationId": "...", "workerId": "...", "payload": { ... } }
```

### Worker → Control plane

| Type | Payload | Meaning |
|------|---------|---------|
| `REGISTER` | workerId, labels, capabilities, **inventory** (skills, MCP servers, providers, tool names) | Join the pool / re-advertise on change |
| `HEARTBEAT` | load, activeSessions, healthy | Liveness + load signal |
| `EVENT` | a serialized `RuntimeEvent` | Stream a turn event for relay to clients |
| `TRANSCRIPT_APPEND` | sessionId, `TranscriptEntry` | Durable transcript delta for the control plane to persist |
| `TURN_RESULT` | sessionId, output, completed, tokensUsed | Terminal result of a dispatched turn |
| `ASK_USER` | sessionId, question, choices | Forward an `AskUserRequestedEvent` (relayed to client) |
| `PROVISION_RESULT` | requestId, ok, installedRef, message | Outcome of a skill/MCP install |

### Control plane → Worker

| Type | Payload | Meaning |
|------|---------|---------|
| `DISPATCH_TURN` | sessionId, `TurnRequest` (input, effectivePrompt, availableTools, model, options) | Run one turn |
| `RESUME_SESSION` | sessionId, full transcript | Rebind after failover — rebuild warm context |
| `PROVISION` | requestId, type (skill\|mcp), ref, args | Install a skill or MCP server on this worker |
| `CANCEL` | sessionId / turnId | Stop an in-flight turn |
| `PING` | — | Liveness check |

`RuntimeEvent` already has a sealed type set and `EventSerializer` (hand-built
JSON) — the `EVENT` frame reuses both as-is.

## End-to-End Turn Flow

```
Client                Control Plane                         Worker (bound)
  │  POST /sessions/{id}/turns {input}                          │
  ├──────────────────────▶│                                    │
  │                        │ look up session→worker binding     │
  │                        │ (none → scheduler picks + persists)│
  │                        │ DISPATCH_TURN(TurnRequest) ───────▶│
  │   202 Accepted         │                                    │ executeTurn():
  │◀──────────────────────┤                                    │  prompt→model→tools→loop
  │                        │   ◀──────── EVENT(turn_started) ───┤
  │  WS: turn_started      │                                    │
  │◀═══════ relay ═════════┤   ◀── EVENT(tool_requested) … ─────┤
  │  WS: tool_* , tokens   │                                    │  (file/shell/git run HERE)
  │◀═══════ relay ═════════┤   ◀── TRANSCRIPT_APPEND ───────────┤
  │                        │ persist transcript delta → DB      │
  │                        │   ◀──────── TURN_RESULT ───────────┤
  │  WS: turn_completed    │ mark turn done                     │
  │◀═══════ relay ═════════┤                                    │
```

### Contract evolution: turns become async

Today `POST /sessions/{id}/turns` **blocks** and returns the final output
(`SessionController`). With execution on a remote worker plus event streaming,
the natural model is **202 Accepted + stream over the client WS**.

To keep dummy clients simple and backward-compatible, the control plane
supports both:

- **Streaming (preferred):** returns `202` with a `turnId`; the client renders
  events from its WS subscription and sees `turn_completed`.
- **Synchronous (compat):** the control plane internally awaits `TURN_RESULT`
  and returns the final output in the HTTP response, preserving today's shape.

## Sticky Sessions & Failover

- **Binding:** a new durable table `session_bindings(session_id, worker_id,
  bound_at, last_active)`. The scheduler binds on first turn and routes every
  subsequent turn for that session to the same worker.
- **Warm cache:** the bound worker keeps the live `Transcript` in memory, so
  follow-up turns skip a DB reload. This is an optimization only.
- **Source of truth:** the DB. Every `TRANSCRIPT_APPEND` is persisted by the
  control plane, so no state is lost if a worker dies.
- **Failover:** heartbeat timeout → mark worker dead → unbind its sessions. The
  next turn for an orphaned session rebinds to a healthy worker; the control
  plane sends `RESUME_SESSION` with the transcript replayed from the DB to
  rebuild warm context. Stickiness resumes against the new worker.
- **No worker available:** the control plane queues the dispatch and surfaces a
  `worker_state_changed`/pending signal to the client; the turn dispatches when
  capacity appears.

## Per-Agent Skills & MCP (Auditing)

Each worker owns a distinct, auditable inventory. This is the mechanism behind
"the CLI can install an MCP or skill specifically for that agent."

- On `REGISTER` (and on any change) a worker advertises its inventory: skill
  IDs + origins, MCP server names/endpoints, providers, and tool names.
- `GET /api/v1/workers` and `GET /api/v1/workers/{id}` expose the live roster
  and each worker's inventory to clients.
- `POST /api/v1/workers/{id}/provision { type: skill|mcp, ref, args }` →
  control plane sends `PROVISION` to that worker → worker installs (writes into
  its skills directory / registers the MCP server) → `PROVISION_RESULT`.
- Every provision is written to `worker_audit(worker_id, actor, type, ref,
  result, at)`. Inventory changes are therefore traceable to who/what/when.

## Where Each Concern Moves

Derived from the current in-process seams (see the seam table below):

| Concern | `hosted` today | `control-plane` | `worker` |
|---------|----------------|-----------------|----------|
| REST client API (`app.api`) | server | ✅ keeps | — |
| Client WS (`app.ws`) | server | ✅ keeps | — |
| JPA sessions/tasks/transcripts | server | ✅ owns | — (streams deltas) |
| Worker registry / scheduler / relay | — | ✅ **new** | connects as WS client |
| Turn engine (`runtime.query`) | server | ❌ removed | ✅ owns |
| Tool registry + builtin file/shell/git (`runtime.tools`) | server | ❌ removed | ✅ owns (runs locally) |
| Skills + MCP bridge (`runtime.skills`) | server | ❌ | ✅ per-agent, audited |
| Providers / LLM keys (`runtime.provider`) | server | ❌ **none** | ✅ owns |
| Permissions / planning | server | policy only | ✅ enforces |
| Cost | server | aggregates | ✅ computes, reports |
| Events (`runtime.events`) | in-mem listeners | ✅ relay + fan-out | ✅ emits + ships |
| Agent coordinator/worker nesting | server | — | ✅ in-process within a worker |

## Seams That Become Network Boundaries

These are the concrete in-process call sites that this design splits (verified
against the current code):

| Seam | Today | Becomes |
|------|-------|---------|
| `DefaultWorkerOrchestrator.delegate()` → `turnEngine.executeTurn()` | same JVM/thread | turn runs on the bound worker; dispatched via `DISPATCH_TURN` |
| `DefaultToolRegistry.execute()` → builtin file/shell/git executors | server JVM filesystem/shell | executes on the worker machine's filesystem/shell |
| `InMemoryEventPublisher` → listener callbacks | in-process listeners | worker emits → ships `EVENT` → control-plane relay → client WS |
| `SessionController` turn endpoint (blocking) | returns output inline | `202` + WS stream (sync compat path retained) |
| `ModelProvider.complete()` | server holds keys | worker holds keys; control plane never calls a model |

## Security

- **Worker authentication:** each worker authenticates to the control plane on
  the WS handshake (shared secret today via the existing `app.auth` API keys;
  mTLS or per-worker identity recommended for production). Reuses the auth
  filter pattern already present for clients.
- **Key isolation:** LLM provider keys live only on workers. A compromised
  control plane leaks no model credentials.
- **Blast radius:** tools run on workers, so untrusted bash/file operations are
  isolated to a worker sandbox, never the orchestration server.
- **Provisioning is privileged:** installing skills/MCP changes what a worker
  can do — gated and fully audited (`worker_audit`).
- **Untrusted content:** transcript/tool output flowing back through `EVENT`
  frames is data, not instructions to the control plane — the relay never
  executes it.

## Non-Goals (this iteration)

- Cross-worker session migration mid-turn (only on failover between turns).
- A new message broker (Kafka/NATS/RabbitMQ) — explicitly chose WS reuse.
- Moving the agent coordinator/worker nesting across machines — it stays
  in-process within a single worker for now.
- Replacing the central DB with per-worker durable stores.

## Open Questions

1. **Routing policy** — round-robin, least-loaded, or label/capability match
   (e.g. "needs the `github` MCP")? Start with capability-match → least-loaded.
2. **Sync turn timeout** — how long the compat path waits for `TURN_RESULT`
   before returning 202.
3. **Worker auth strength** — shared secret for v1 vs mTLS for production.
4. **Inventory drift** — does a provision require a worker quiesce, or is it
   hot? Default: hot for MCP/skill add, recorded in audit.

## See Also

- `DISTRIBUTED_MIGRATION_PLAN.md` — the numbered, reusable step plan.
- `ARCHITECTURE.md` — the current (hosted) runtime architecture.
- `PARITY_VERIFICATION.md` — behavioral contract that must survive the split.
