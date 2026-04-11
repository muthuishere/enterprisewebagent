# enterprisewebagent

Java-first agent runtime product with a Spring-hosted backend, React web UI, and a lightweight Picocli CLI.

## Repository Shape

```
apps/
├── server/    ← Spring Boot runtime host (Java 21, Gradle)
├── frontend/  ← React 19 + Vite + TypeScript (Bun)
└── cli/       ← Picocli CLI (Java 21, Gradle, no Spring)
docs/          ← product, requirements, and technical planning docs
```

Each app builds independently. No multi-module Gradle.

## Quick Start

```bash
# Check tools
task status

# Install frontend deps
task install

# Start server + frontend in dev mode
task dev

# Run all tests
task test

# Build everything
task build
```

## Architecture

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full runtime architecture.

### Key Boundaries

- **Server** owns the runtime core: prompt assembly, tool registry, turn execution, worker orchestration, session/task state, runtime events
- **Spring AI** is behind the provider boundary — used for LLM access and MCP, never for prompt design or orchestration
- **CLI** is a Picocli app that talks to the server via HTTP/WebSocket — same contracts as the frontend
- **Frontend** is a React app consuming the runtime's API and WebSocket streaming

### Runtime Modules (apps/server)

| Package | Responsibility |
|---------|---------------|
| `runtime.prompt` | 5-level prompt precedence, 12-surface catalog, section caching |
| `runtime.tools` | Tool registry with deny→mode→sort pipeline, 6 built-in tools |
| `runtime.query` | Turn engine (tool-call loop, max 10 iterations), streaming, compaction |
| `runtime.agents` | Coordinator/worker orchestration, depth-limited delegation |
| `runtime.skills` | Markdown skill loading with frontmatter, PROJECT→USER→MANAGED |
| `runtime.events` | Sealed RuntimeEvent interface, 8 event types, async publisher |
| `runtime.memory` | Session memory, memory store |
| `runtime.provider` | Spring AI adapter, model provider registry |
| `runtime.session` | Session lifecycle, compaction manager |
| `runtime.tasks` | Task lifecycle, status tracking |
| `app.api` | REST controllers (Session, Task, Config) |
| `app.ws` | WebSocket streaming handler |
| `app.config` | Spring bean wiring |

## Current State

**Phase 1 complete** — all 17 handoff steps implemented.

| Metric | Value |
|--------|-------|
| Server source files | 98 |
| Server test files | 33 (all green) |
| CLI source files | 7 |
| CLI test files | 3 (all green) |
| Frontend modules | 96 (builds clean, 266KB bundle) |
| Parity score | 12/14 PASS, 1 GAP, 1 PARTIAL |

See [`docs/PARITY_VERIFICATION.md`](docs/PARITY_VERIFICATION.md) for the full parity checklist.

### Known Gaps

1. **Session persistence** — currently in-memory only; `SessionManager` interface is ready for a durable implementation
2. **Ask-user event** — `AskUserTool` works but no dedicated WebSocket event; clients parse model text
3. **Auth** — deferred until runtime boundaries are stable

## Key Documents

| Document | Purpose |
|----------|---------|
| [`docs/INDEX.md`](docs/INDEX.md) | Reading order for all docs |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Runtime architecture and data flows |
| [`docs/PRODUCT.md`](docs/PRODUCT.md) | Product vision |
| [`docs/PRD.md`](docs/PRD.md) | Product requirements |
| [`docs/IMPLEMENTATION_HANDOFF.md`](docs/IMPLEMENTATION_HANDOFF.md) | 17-step implementation guide |
| [`docs/PARITY_VERIFICATION.md`](docs/PARITY_VERIFICATION.md) | Parity checklist (Step 17) |
| [`docs/lld-core-parity.md`](docs/lld-core-parity.md) | Low-level parity contract |
| [`docs/prompts/`](docs/prompts/) | 12 prompt surface definitions |

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Server runtime | Java 21, Spring Boot 4 RC2 |
| AI integration | Spring AI BOM 2.0 M4 |
| CLI | Picocli 4.7.6, Java 21 HttpClient, Gson |
| Frontend | React 19, TypeScript, Vite 6, Bun |
| Persistence | H2 (dev), Postgres (production) |
| Task runner | Taskfile v3 with env-specific includes |
