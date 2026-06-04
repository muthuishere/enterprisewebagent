# The Agent, Licensing & Install Flow

Companion to `DISTRIBUTED_ARCHITECTURE.md`. This document details the **Go
agent** (the runtime that does the work), the **license model**, the **install
flow**, and the **LLM proxy + config-distribution** contracts that connect the
agent to the server.

## Why a Go agent (and not Spring)

The agent is something a customer **downloads and installs on a machine**, and
that we **license**. That argues for:

- a **single static binary**, cross-compiled for Linux/macOS/Windows, no JVM or
  runtime to install;
- a **small footprint** that "just runs" on arbitrary boxes;
- fast startup and low idle cost for many agents.

The server stays Spring Boot — it benefits from Spring AI as the provider
gateway. The CLI stays Picocli. Only the **agent** is Go.

## What the agent owns

The agent is the full runtime ported to Go, preserving the behavior contract in
`PARITY_VERIFICATION.md`:

- **Turn loop** — prompt → server proxy → parse → run tools → loop (max 10
  iterations); emits the same `RuntimeEvent` types.
- **Prompt assembly** — 5-level precedence, 12-surface catalog, section caching,
  plus a **local environment block** (cwd, OS, git state, file listing). Skill
  content + which capabilities exist come from the server's `CONFIG_UPDATE`.
- **Tools** — bash/file/git/glob/grep executed **locally**; deny→mode→permission
  pipeline; alphabetical, deterministic ordering.
- **MCP** — hosts its own MCP servers on the machine.
- **Skills** — the per-agent skill set (content distributed by the server).
- **Live session state** — the working directory + in-flight transcript live on
  the agent; deltas are synced up to the server.

The agent holds **no provider keys, no DB**, and calls the server proxy for
every model completion.

## Agent lifecycle

```
 install ──▶ activate (license) ──▶ REGISTER ──▶ CONFIG_PULL ──▶ run ──▶ SESSION_SYNC ──▶ … ──▶ heartbeat
   │             │                     │              │           │            │
 binary +    token validated      authenticate    capability   turn loop   stream events
 token        by server           + advertise      profile      local      + transcript up
                                   inventory        applied
```

1. **Install** — the binary lands on a machine (see install flow below) with a
   **license token**.
2. **Activate** — the agent validates the token with the server.
3. **Register** — opens one outbound WebSocket, sends `REGISTER` (license +
   baked-in inventory + labels). The server authenticates and admits it.
4. **Config pull** — receives `CONFIG_UPDATE`: the capability profile (extra
   tools/MCP/skills) the server provisions for this agent.
5. **Run** — services `SESSION_INPUT` from clients; assembles prompts; calls the
   proxy; runs tools locally; streams `SESSION_SYNC` up.
6. **Heartbeat** — periodic liveness + load; absence triggers failover.

## License model — identity + entitlements

A license is **identity + entitlements**, issued and signed by the server.

| Field | Purpose |
|-------|---------|
| `agentId` / `customerId` | **Identity** — who this agent belongs to |
| `seats` / `maxAgents` | How many agents may be active concurrently |
| `expiry` | Validity window |
| `tier` / `capabilities` | Which capability tiers the agent may run (tools/MCP/skills classes) |
| `tokenBudget` (optional) | Usage cap enforced at the proxy |

**Enforced at two points:**

1. **Connect** — the `REGISTER` handshake validates the token (signature,
   expiry, seat availability) before admitting the agent.
2. **Every `LLM_REQUEST`** — the proxy re-checks the entitlement (tier, budget)
   before forwarding to a provider, and **meters** the tokens used.

This dual enforcement is the point of the architecture: because every turn must
cross the proxy for its model call, licensing/metering/capping are unavoidable
and centralized. **Revocation** takes effect on the next connect or proxy call.

## Install flow (from a thin client)

A user obtains a licensed agent from a client and installs it on a target
machine:

```
Client (CLI or Web)            Server                       Target machine
  │  request licensed agent        │                              │
  ├───────────────────────────────▶│ issue license token +        │
  │                                 │ install command/script       │
  │  install command + token       │                              │
  │◀────────────────────────────────┤                             │
  │  (user runs it on the box) ─────┼─────────────────────────────▶│ download binary,
  │                                 │                              │ install with token
  │                                 │   ◀──────── REGISTER ─────────┤ agent connects
  │  agent now visible in roster    │                              │
```

- `POST /api/v1/agents/provision-install` → server returns a one-time install
  command + a license token bound to the requesting customer/entitlement.
- The user runs it on the target machine (download + install + token); the agent
  registers and appears in `GET /api/v1/agents`.
- The Java/Picocli CLI and the Go agent can also be **bundled** so a single
  install puts both on the same machine when selected.

## Config distribution — the "single update"

Agents ship with **baked-in defaults** and are refined by the server at runtime.

- `CONFIG_UPDATE` carries the agent's **capability profile**: enabled tools, MCP
  servers to host (e.g. install + launch a filesystem MCP), and skill content.
- **Provisioning is per-agent:** `POST /api/v1/agents/{id}/provision { type:
  skill|mcp, ref, args }` → server sends `CONFIG_UPDATE` to that agent → the
  agent installs locally (writes skill files, launches the MCP server) →
  re-advertises its inventory.
- Updates are **version-stamped**; the agent `CONFIG_PULL`s the latest on
  connect and applies hot (default) — see open question on quiesce semantics.
- Every provision is written to `agent_audit(agent_id, actor, type, ref, result,
  at)` for traceability.

## The LLM proxy contract (detail)

```
Agent                                   Server proxy
  │  LLM_REQUEST {                          │
  │    sessionId, model,                    │  1. validate license + entitlement
  │    prompt: [PromptSection…],            │  2. check token budget / tier
  │    options { temperature, maxTokens } } │  3. ProviderModels.resolve(model) → providerId
  ├─────────────────────────────────────────▶│  4. ModelProvider.stream(ModelRequest)
  │   ◀── LLM_RESPONSE (chunk) ──────────────┤  5. meter tokens per chunk
  │   ◀── LLM_RESPONSE (chunk) ──────────────┤  6. on completion: record usage,
  │   ◀── LLM_RESPONSE (done, usage) ────────┤     decrement budget
```

Reuses, unchanged, from `runtime.provider`:
- `ModelProvider.complete(ModelRequest)` / `stream(ModelRequest)`
- `DefaultModelProviderRegistry`, `SpringAiModelProvider`, `ProviderModels`

New on the server: the entitlement check + token meter wrapped around the
provider call, and the WS framing (`LLM_REQUEST`/`LLM_RESPONSE`).

## Protocol message reference

See `DISTRIBUTED_ARCHITECTURE.md` → *Agent ↔ Server Protocol* for the full
message table (`REGISTER`, `HEARTBEAT`, `LLM_REQUEST`, `SESSION_SYNC`,
`CONFIG_PULL`, `ASK_USER`, `LLM_RESPONSE`, `CONFIG_UPDATE`, `SESSION_INPUT`,
`CANCEL`, `PING`).

## Parity obligation

The Go port of the turn loop, prompt assembly, tool pipeline, and skills must
satisfy the same checklist as `PARITY_VERIFICATION.md`: prompt precedence,
section caching, the 12-surface catalog, tool ordering, deny-rule filtering,
the turn loop (max 10), compaction, and skill load order. The migration plan
gates the cutover on a cross-implementation parity test.
