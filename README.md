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

## Key Documents

- `docs/INDEX.md`
- `docs/PRODUCT.md`
- `docs/PRD.md`
- `docs/fsd-enterprisewebagent.md`
- `docs/trd-enterprisewebagent.md`
- `docs/SCENARIOS.md`
- `docs/NON_GOALS.md`
- `docs/EXECUTION_PIPELINE.md`

## Architecture

- **Server** owns the runtime core: prompt assembly, tool registry, turn execution, worker orchestration, session/task state, runtime events
- **Spring AI** is behind the provider boundary — used for LLM access and MCP, never for prompt design or orchestration
- **CLI** is a Picocli app that talks to the server via HTTP/WebSocket — same contracts as the frontend
- **Frontend** is a React app consuming the runtime's API and WebSocket streaming

## Current State

Scaffold stage — all three apps compile and run. Runtime module interfaces are defined. Implementation of the handoff steps is next.
