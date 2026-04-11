# enterprisewebagent Agent Notes

## Working Principles

- Treat `enterprisewebagent` as its own runtime product, not as a thin Spring AI wrapper.
- Keep runtime behavior inside shared Java modules rather than scattering it across controllers or clients.
- Keep the React UI and Java CLI aligned to the same session, task, and streaming contracts.
- Use Spring AI as a provider and integration layer, not as the product architecture.
- Preserve the current agent core prompts, tools, instruction layering, cache-aware behavior, and runtime semantics as closely as possible.
- **Parity is sacred** — Phase 1 reproduces the current core faithfully. Improvements are Phase 2.

## Repository Layout

```
enterprisewebagent/
├── AGENTS.md
├── README.md
├── Taskfile.yml                  ← root orchestrator (includes env-specific Taskfiles)
├── Taskfile.local.yml            ← local dev (postgres, etc.)
├── Taskfile.dev.yml              ← dev deployment
├── Taskfile.production.yml       ← prod deployment
├── .env.example
├── docs/                         ← product, requirements, and technical planning docs
├── apps/
│   ├── server/                   ← Spring Boot runtime host (Gradle, Java 21)
│   │   ├── build.gradle
│   │   ├── Taskfile.yml
│   │   └── src/main/java/com/enterprisewebagent/
│   │       ├── runtime/
│   │       │   ├── prompt/       ← prompt precedence, section assembly, cache
│   │       │   ├── tools/        ← tool registry, filtering, pool assembly
│   │       │   ├── query/        ← turn execution, streaming, compaction
│   │       │   ├── agents/       ← coordinator mode, worker orchestration
│   │       │   ├── skills/       ← markdown skill loading, instruction expansion
│   │       │   ├── memory/       ← session memory, memory prompts
│   │       │   ├── events/       ← transport-agnostic runtime events
│   │       │   ├── provider/     ← Spring AI provider adapter
│   │       │   ├── tasks/        ← task lifecycle
│   │       │   └── session/      ← session state
│   │       └── app/
│   │           ├── api/          ← REST controllers
│   │           ├── ws/           ← WebSocket streaming
│   │           └── config/       ← Spring configuration
│   ├── frontend/                 ← React 19 + Vite + TypeScript (Bun)
│   │   ├── package.json
│   │   ├── Taskfile.yml
│   │   └── src/
│   │       ├── lib/              ← config, http, ws helpers
│   │       ├── domain/           ← session/, tasks/ (pages + hooks + types)
│   │       └── app/              ← App.tsx, routing
│   └── cli/                      ← Picocli CLI (Gradle, Java 21, NO Spring)
│       ├── build.gradle
│       ├── Taskfile.yml
│       └── src/main/java/com/enterprisewebagent/cli/
```

## Key Boundaries

- **Server** owns the runtime core. Spring hosts it; Spring AI is the provider adapter.
- **CLI** is a lightweight Picocli app that talks to the server via HTTP/WebSocket. No Spring dependency.
- **Frontend** is a React app that consumes the same API/WebSocket contracts as the CLI.
- Each app builds independently — no multi-module Gradle.

## Early Priorities

All completed in Phase 1:

- ✅ map the existing core prompt, tool, and instruction behavior before making architectural substitutions
- ✅ lock runtime-core boundaries
- ✅ define task and session models
- ✅ define transport-agnostic runtime events
- ✅ keep background execution inside the Spring app first
- ⏳ defer auth until the runtime boundaries are stable (boundaries now stable — auth is next)
