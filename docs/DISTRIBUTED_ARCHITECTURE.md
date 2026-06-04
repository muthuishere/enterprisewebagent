# Distributed Architecture — Fat Agent + LLM-Proxy Server

> **Supersedes the earlier draft of this file.** An earlier version described a
> "thin control plane + fat worker that owns the LLM." That model was inverted
> during design review. This document is the current target.

## Purpose

Today the runtime is a single Spring Boot process (`hosted` mode): one JVM hosts
the client API, the turn engine, the tools, the LLM providers, and the session
store. This document specifies the target where:

- **The agent is the full runtime, installed on a machine, and "just runs."** A
  licensed Go binary that owns the turn loop, prompt assembly, tools, MCP, and
  skills — and does the real work locally on its own machine.
- **The server is an LLM proxy + control plane + system-of-record.** It holds
  the provider keys and proxies model calls (metering + enforcing licenses),
  issues licenses, distributes agent config, and stores the synced session
  record. It never assembles prompts and never executes tools.
- **The CLI and Web UI are dummy clients.** They drive sessions and render the
  relayed event stream. No business logic lives in a client.

The goal: ship a **licensed agent you install on any machine**, that works
locally and fast, and reaches the server only as an **LLM gateway** — so the
one seam every customer must cross (the model call) is also the licensing,
metering, and key-custody choke point.

This is a specification and a migration target. It does not change behavior on
its own — see `DISTRIBUTED_MIGRATION_PLAN.md` for the numbered steps, and
`AGENT_AND_LICENSING.md` for the agent, license, and install detail.

## Design Decisions (locked)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Where the runtime lives | **Agent** owns turn loop, prompt assembly, tools, MCP, skills | The agent does the work locally → fast, autonomous, no per-tool round trip. |
| Server's role | **LLM proxy + control + system-of-record** | The model call is the one mandatory seam; make it the licensing/metering/key-custody choke point. |
| Prompt assembly | **On the agent**, using config from the server + a local environment block | Assembly needs the live transcript + machine context, both already on the agent. |
| LLM keys | **Server only** | Keys never ship to a customer machine. The agent calls the server proxy, not the provider. |
| Agent↔server transport | **WebSocket**, agent dials *out* | No inbound ports on customer machines; traverses NAT/firewalls. |
| Live stream to clients | **Relayed through the server** | Text event stream is cheap; relay always traverses NAT and serves CLI + web. No P2P/WebRTC/TURN. |
| Session state | **Live on the agent; record synced up to the server DB** | Agent is autonomous; server keeps durable, observable history for thin clients. |
| Session affinity | **Sticky** — a session pins to one agent | Filesystem/working-dir continuity (turn 2's bash sees turn 1's file). |
| Licensing | **Identity + entitlements** (agent/seat count, expiry, capability tiers) | A real licensed product; enforced at connect *and* on every proxy call. |
| Capability provisioning | **Baked-in defaults + runtime push** from server | Agent ships usable; server can install MCP/skills onto a running agent, per agent. |
| CLI | **Picocli (Java) stays**; agent is a separate **Go** binary | Least client churn; Go is ideal for a tiny installable licensed agent. |

## The Shape

```
   CLI (Picocli) ┐                                ┌──────────────────────────────┐
                 │  REST + client WS (relayed)    │  AGENT  (Go, licensed)        │
      Web UI  ───┤                                │  app.runtime = external       │
                 ▼                                │                               │
        ┌──────────────────────┐  agent WS        │  • Turn loop                  │
        │   SERVER              │  (agent dials    │  • Prompt assembly            │
        │ (app.runtime.mode=    │   out)           │  • Tools: bash/file/git       │
        │     proxy)            │◀════════════════▶│  • MCP hosting                │
        │                       │  LLM_REQUEST ───▶│  • Skills                     │
        │ • LLM PROXY (keys,    │◀── LLM_RESPONSE  │  • Live session + working dir │
        │   meter, entitle,     │  SESSION_SYNC ──▶│                               │
        │   route via Spring AI)│  CONFIG_UPDATE ─▶│  ─── reaches server only for: │
        │ • Licensing service   │                  │      LLM · config · sync      │
        │ • Config distribution │                  └──────────────────────────────┘
        │ • Agent registry      │
        │ • System-of-record    │      ┌──────────────┐         (one outbound WS
        │ • Live-stream relay    │─────▶│ Postgres/H2  │          per agent;
        │ • NO prompt assembly  │      │ sessions,    │          more agents =
        │ • NO tool execution   │      │ transcripts, │          more machines)
        └──────────────────────┘      │ licenses,    │
                                       │ agent_audit  │
                                       └──────────────┘
```

Clients only talk to the server. Agents only dial out to the server. Clients
and agents never connect directly — the server relays the live stream.

## Runtime Modes

The **Java server** is selected by the existing `app.runtime.mode` property
(`apps/server/src/main/resources/application.yml:50`, today `hosted`):

| `app.runtime.mode` | Loads | Use |
|--------------------|-------|-----|
| `hosted` (today) | Everything in one JVM | Local dev, single-box, tests. **Unchanged.** |
| `proxy` | LLM proxy + licensing + config + registry + record + relay | Production server. No turn engine, no tools, no prompt assembly. |

The **Go agent is a separate binary**, not a Spring mode. Bean wiring becomes
mode-aware via `@ConditionalOnProperty(app.runtime.mode)`; `hosted` keeps every
bean and stays the default, so nothing breaks for existing users.

## Component Responsibilities

### Agent (Go binary — the runtime)

| Concern | Detail | Ported from |
|---------|--------|-------------|
| Turn loop | prompt → **proxy** → parse → tools → loop (max 10), event emission | `runtime.query` (`DefaultTurnEngine`) |
| Prompt assembly | 5-level precedence, 12-surface catalog, section caching, + local env block | `runtime.prompt` (`PromptCatalog`, `DefaultPromptAssembler`) |
| Tools | bash/file/git/glob/grep run **locally on this machine**; deny→mode→permission pipeline; alphabetical ordering | `runtime.tools` (`DefaultToolRegistry` + builtin) |
| MCP | hosts its own MCP servers locally | MCP bridge |
| Skills | per-agent skill set, content from server config | `runtime.skills` |
| Session state | live transcript + working dir held locally; synced up | `runtime.session` (agent-local) |
| Model access | calls the **server proxy** — never a provider directly, holds no keys | — |
| Link | one outbound WebSocket: register (license + inventory), heartbeat, LLM requests, session sync, config pull | new |

### Server (`app.runtime.mode=proxy`)

| Concern | Detail | Reuses today |
|---------|--------|--------------|
| **LLM proxy** | Holds keys; agent sends an assembled prompt + model id; server enforces entitlements, meters tokens, routes to a provider, streams the completion back | `runtime.provider` (`ModelProvider.complete/stream`, `SpringAiModelProvider`, `ProviderModels`) |
| **Licensing** | Issues licenses (identity + entitlements); enforces on connect and on every proxy call | new (`server.license`) |
| **Config distribution** | Pushes an agent's capability profile (tools/MCP/skills) — baked-in defaults + runtime push | new (`server.config`) |
| **Agent registry** | Tracks connected agents, their inventory, heartbeat | new (`server.agents`) |
| **System-of-record** | Ingests synced session/transcript deltas, persists them | `app.persistence` (`JpaSessionManager`, `JpaTaskManager`) |
| **Live-stream relay** | Receives the agent's live events, fans out to client-WS subscribers of that session | `app.ws` (`StreamingWebSocketHandler`), `runtime.events` (`EventSerializer`) |
| Client API | Session/task drive, history reads, install/license endpoints | `app.api` |
| Prompt assembly / tools | **None.** Never assembles prompts, never executes tools. | — |

### Clients (CLI, Web UI) — dummy

| Allowed | Not allowed |
|---------|-------------|
| Create/resume/close sessions; submit input | Run any tool |
| Read session history from the server DB | Assemble prompts / call a model |
| Subscribe to the server-relayed live stream | Connect directly to an agent |
| Request a licensed agent + install token; provision skill/MCP onto an agent | Make routing/scheduling decisions |

## The LLM Proxy Contract (the central seam)

This is the one mandatory agent→server call and the heart of the licensing model.

```
Agent                          Server (proxy)                       Provider
  │  LLM_REQUEST                    │                                   │
  │  { sessionId, model,            │                                   │
  │    prompt: [PromptSection…],    │                                   │
  │    options }                    │                                   │
  ├────────────────────────────────▶│ 1. authenticate license          │
  │                                  │ 2. enforce entitlement (tier,    │
  │                                  │    seat, expiry, token budget)   │
  │                                  │ 3. resolve provider for `model`  │
  │                                  │    (ProviderModels routing)      │
  │                                  │ 4. ModelProvider.stream(req) ────▶│
  │   ◀──── LLM_RESPONSE (stream) ───┤ 5. meter tokens as they flow ◀────┤
  │                                  │ 6. record usage for billing      │
```

- The server **never sees a raw user prompt it assembled** — it receives an
  already-assembled `PromptSection` list and forwards it. It is a gateway.
- Metering and entitlement enforcement live here because **every** turn crosses
  this seam exactly once per model call.
- This restores the project's stated principle verbatim: *Spring AI is behind
  the provider boundary — used for LLM access and MCP, never for prompt design
  or orchestration.* In `proxy` mode the server is **only** that boundary.

## Prompt Assembly on the Agent

Assembly stays on the agent because it consumes the live transcript and machine
context that live there. It draws on two server-supplied inputs:

- **Capability profile** (configured from the server, pushed via `CONFIG_UPDATE`):
  which tools/MCP/skills this agent has, and the skill content to inject.
- **Local environment block** (only the agent knows): cwd, OS, git state, file
  listing — folded into the environment section at assembly time.

The 5-level precedence, the 12-surface catalog, and section caching port from
`runtime.prompt` unchanged in behavior; only their *host* moves to the agent.

## Agent ↔ Server Protocol (JSON over one outbound WebSocket)

```json
{ "type": "<TYPE>", "correlationId": "...", "agentId": "...", "payload": { ... } }
```

| Direction | Type | Payload | Meaning |
|-----------|------|---------|---------|
| A→S | `REGISTER` | license token, inventory (tools/MCP/skills), labels | Join; authenticate; advertise capability |
| A→S | `HEARTBEAT` | load, activeSessions, healthy | Liveness |
| A→S | `LLM_REQUEST` | sessionId, model, assembled prompt, options | Ask the proxy for a completion |
| A→S | `SESSION_SYNC` | sessionId, `RuntimeEvent` / `TranscriptEntry` deltas | Stream events up for relay + persistence |
| A→S | `CONFIG_PULL` | currentVersion | Request the latest capability profile |
| A→S | `ASK_USER` | sessionId, question, choices | Forward an ask-user up to the client |
| S→A | `LLM_RESPONSE` | streamed completion chunks | Proxy result |
| S→A | `CONFIG_UPDATE` | capability profile (the "single update") | Provision/refresh tools/MCP/skills |
| S→A | `SESSION_INPUT` | sessionId, user input | A client submitted input for a bound session |
| S→A | `CANCEL` / `PING` | — | Control |

`RuntimeEvent` + `EventSerializer` are reused as the wire format for the events
inside `SESSION_SYNC` (and for fan-out to clients).

## End-to-End Turn Flow

```
Client            Server (proxy)                         Agent (bound, sticky)
  │  POST /sessions/{id}/input {text}                          │
  ├────────────────────▶│  resolve session→agent binding       │
  │                      │  SESSION_INPUT ─────────────────────▶│ assemble prompt (local
  │  202 Accepted        │                                      │   transcript + env + config)
  │◀─────────────────────┤                                     │
  │                      │   ◀──────────── LLM_REQUEST ─────────┤
  │                      │ license + entitlement + meter        │
  │                      │ route → ModelProvider.stream ───────▶│ (provider)
  │                      │   LLM_RESPONSE (stream) ────────────▶│ parse → tool calls
  │                      │                                      │ run bash/file/git LOCALLY
  │  WS: events          │   ◀──────────── SESSION_SYNC ────────┤ (events + transcript deltas)
  │◀═══ relay ═══════════┤ persist deltas → DB                  │ loop until done (max 10)
  │  WS: turn_completed  │   ◀──────────── SESSION_SYNC ────────┤
  │◀═══ relay ═══════════┤                                      │
```

The server is a hop only for the **model call** and the **event relay/record** —
not for tool execution, which never leaves the agent.

## Session Sync, History & Live Viewing

- The agent owns the live transcript and pushes `SESSION_SYNC` deltas up
  continuously. The server persists them via `JpaSessionManager` — the DB is the
  durable system-of-record.
- **History:** clients read past sessions from the server (`GET /sessions/{id}`).
- **Live:** clients subscribe to the server's client WS; the relay forwards the
  agent's live events. Clients never connect to agents.

## Sticky Sessions & Failover

- **Binding:** `session_bindings(session_id, agent_id, bound_at, last_active)`,
  persisted. Every input for a session routes to its bound agent.
- **Why sticky:** the working directory (files, git checkout) lives on the
  agent; successive turns must hit the same machine.
- **Failover caveat (accepted):** the synced transcript survives an agent loss,
  but **uncommitted on-disk working state does not** — a rebind to a new agent
  starts with a clean working dir. Mitigations: commit-often, or a workspace
  snapshot/restore step. Documented as a known limitation, not solved here.

## Licensing (summary — full detail in `AGENT_AND_LICENSING.md`)

- A license carries **identity** (which agent/customer) + **entitlements**
  (agent/seat count, expiry, capability tier, token budget).
- Enforced at **two points**: the WS `REGISTER` handshake, and **every**
  `LLM_REQUEST` at the proxy (so usage can be metered and capped).
- Keys for actual providers never leave the server; a compromised agent leaks
  no provider credentials.

## Security

- **Key custody:** provider keys live only on the server; the agent has none.
- **Blast radius:** tool execution is on the agent (a sandboxed customer
  machine); a compromised server executes no tools.
- **Auth:** the agent authenticates with its license token on the WS handshake;
  the proxy re-checks entitlement per call. Reuses the `app.auth` filter pattern.
- **IP tradeoff (accepted):** prompt catalog + skill content ship to the agent
  as licensed, entitlement-gated config — that IP sits on a customer machine.
- **Untrusted content:** tool output flowing up in `SESSION_SYNC` is data the
  server persists/relays, never executes.

## What Moves Where (from today's `hosted` code)

| Concern | `hosted` today | `proxy` server | Go agent |
|---------|----------------|----------------|----------|
| LLM provider + keys (`runtime.provider`) | server | ✅ proxy (keys, meter, route) | ❌ calls proxy |
| Turn engine (`runtime.query`) | server | ❌ | ✅ owns |
| Prompt assembly (`runtime.prompt`) | server | ❌ | ✅ owns |
| Tools + builtin file/shell/git (`runtime.tools`) | server | ❌ | ✅ runs locally |
| Skills + MCP (`runtime.skills`) | server | ❌ (distributes config) | ✅ hosts |
| Sessions/tasks/transcripts (`app.persistence`) | server | ✅ system-of-record (synced up) | live copy, synced |
| Client API + WS (`app.api`, `app.ws`) | server | ✅ keeps + relay | — |
| Licensing / registry / config dist | — | ✅ **new** | authenticates / pulls config |
| Events (`runtime.events`, `EventSerializer`) | in-mem | ✅ relay format + fan-out | ✅ emits + ships up |

## Seams That Change

| Seam | Today | Becomes |
|------|-------|---------|
| `DefaultTurnEngine.callModel()` → `ModelProvider.complete()` | in-JVM provider call | `LLM_REQUEST` to the server proxy; keys + metering server-side |
| `DefaultToolRegistry.execute()` → file/shell/git executors | server JVM | runs on the agent's machine (no change in code, new host = Go port) |
| `DefaultPromptAssembler.assemble()` | server | runs on the agent with server-supplied config + local env |
| `InMemoryEventPublisher` → listeners | in-process | agent emits → `SESSION_SYNC` up → server relay → client WS |
| `SessionController` turn endpoint (blocking) | returns output inline | `202` + relayed WS stream (sync compat path may be retained) |

## Non-Goals (this iteration)

- Direct agent↔client connectivity (WebRTC/UDP/TURN) — explicitly chose server relay.
- Moving prompt catalog/skill IP off the customer machine — accepted tradeoff.
- Cross-agent session migration mid-session (only on failover, with the
  working-dir caveat).
- Replacing the central record DB with per-agent durable stores.

## Open Questions

1. **Metering granularity** — per-token, per-turn, or per-session for billing?
2. **Config consistency** — does a `CONFIG_UPDATE` require the agent to quiesce
   in-flight sessions, or is it hot? Default: hot, version-stamped.
3. **Sync-compat turns** — keep a synchronous REST turn (server awaits final
   event) for simple clients, or stream-only?
4. **Workspace durability** — offer an optional snapshot/restore to soften the
   failover working-dir caveat?

## See Also

- `AGENT_AND_LICENSING.md` — the Go agent, license/entitlement model, install +
  runtime-provisioning flow, and the full proxy contract.
- `DISTRIBUTED_MIGRATION_PLAN.md` — the numbered step plan.
- `ARCHITECTURE.md` — the current (hosted) runtime architecture.
- `PARITY_VERIFICATION.md` — the behavioral contract the agent port must preserve.
