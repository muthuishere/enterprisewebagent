# enterprisewebagent

Java-first agent runtime product with a Spring-hosted backend, React web UI, and a lightweight Picocli CLI. Functionally modeled after Claude Code with enterprise-grade architecture.

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

- **Server** owns the runtime core: prompt assembly, tool registry, turn execution, worker orchestration, session/task state, runtime events, planning, permissions, cost tracking, slash commands
- **Spring AI** is behind the provider boundary — used for LLM access and MCP, never for prompt design or orchestration
- **CLI** is a Picocli app that talks to the server via HTTP/WebSocket — same contracts as the frontend
- **Frontend** is a React app consuming the runtime's API and WebSocket streaming

### Runtime Modules (apps/server)

| Package | Responsibility |
|---------|---------------|
| `runtime.prompt` | 5-level prompt precedence, 12-surface catalog, section caching |
| `runtime.tools` | Tool registry with deny→mode→permission pipeline, 23 built-in tools + MCP bridge |
| `runtime.commands` | Slash command system — 15 commands (/help, /plan, /review, /cost, /model…) |
| `runtime.query` | Turn engine (tool-call loop), extended thinking, transcript compaction |
| `runtime.agents` | Coordinator/worker orchestration, custom agent definitions |
| `runtime.teams` | Multi-agent team collaboration, message passing |
| `runtime.planning` | Plan mode (4 phases), step approval, plan-aware tool filtering |
| `runtime.permissions` | Auto-approval classifier, bash safety, file path checking, denial tracking |
| `runtime.cost` | USD cost per model/turn/session, pricing catalog |
| `runtime.skills` | Markdown skill loading with frontmatter, PROJECT→USER→MANAGED |
| `runtime.events` | Sealed RuntimeEvent interface, 10 event types, async publisher |
| `runtime.memory` | MEMORY.md persistence, auto-summarization, session memory |
| `runtime.hooks` | Pre/post hooks for turns, tools, files, sessions (7 hook types) |
| `runtime.provider` | 5 LLM providers (OpenAI, Anthropic, Ollama, Copilot, Codex), prefix routing |
| `runtime.session` | Session lifecycle, tagging, export (markdown/JSON/summary) |
| `runtime.tasks` | Task lifecycle, JPA persistence |
| `app.api` | REST controllers (Session, Task, Config, Cost, Permission) |
| `app.ws` | WebSocket streaming handler |
| `app.config` | Spring bean wiring, auth, observability |

## Current State

**Phase 4 complete** — functional parity with core Claude Code features.

| Metric | Value |
|--------|-------|
| Runtime source files | 187 |
| App layer source files | 28 |
| Test files | 104 (644 tests, 0 failures) |
| Built-in tools | 23 + MCP bridge |
| Slash commands | 15 |
| LLM providers | 5 |
| Event types | 10 |
| Frontend modules | 100 (283KB bundle) |

### Completed Phases

| Phase | Scope | Status |
|-------|-------|--------|
| **1 — Core Parity** | 17 handoff steps: prompts, tools, events, agents, sessions, transport | ✅ |
| **2 — Production** | JPA persistence, 5 providers, auth, observability, CLI packaging | ✅ |
| **3 — Providers** | ProviderModels catalog, ModelRegistry, prefix routing, health detection | ✅ |
| **4 — Functional** | 23 tools, 15 commands, plan mode, thinking, permissions, cost, memory, git, hooks, teams | ✅ |

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
| Persistence | H2 (dev), Postgres (production), Flyway |
| Task runner | Taskfile v3 with env-specific includes |
