# Technical Requirements Document - enterprisewebagent

## Architecture Summary

`enterprisewebagent` should be built as:

- one shared Java runtime core
- one Spring Boot application that hosts it
- one Java CLI packaged with `jlink`
- one React frontend that consumes the same runtime contracts

## Primary Technical Constraint

The current agent core is the behavioral reference implementation. `enterprisewebagent` should preserve that behavior as closely as possible.

Parity applies to:

- prompt composition and prompt intent
- prompt precedence and instruction layering
- cache-aware prompt behavior where the current runtime already uses it
- tool registry behavior and tool-call semantics
- turn execution model
- coordinator and worker semantics where present
- user interaction style exposed through the runtime

## Runtime Ownership

The runtime core should own:

- prompt assembly
- query and turn execution
- tool registry
- orchestration roles
- session and task state model
- provider abstraction
- runtime event model

Spring AI should remain behind a provider adapter boundary.

The current agent core should be treated as the source behavior for these areas. The Spring host should run that behavior, not reinterpret it.

## Recommended Modules

- `runtime-prompt`
- `runtime-query`
- `runtime-tools`
- `runtime-agents`
- `runtime-memory`
- `runtime-provider`
- `runtime-events`
- `runtime-tasks`

## Spring App Responsibilities

- host runtime-core
- expose HTTP APIs
- expose WebSocket streaming
- persist session and task state
- run in-process background execution

The Spring app should host the runtime, not redefine core prompt behavior, tool semantics, or orchestration logic in controller or service layers.

## CLI Responsibilities

- use the same shared runtime contracts
- provide terminal UX
- support automation entrypoints
- package through `jlink`

The CLI should surface the same runtime behavior as the hosted product. It must not become a parallel interpretation of prompts, tools, or instructions.

## Frontend Responsibilities

- session view
- task/status view
- model and configuration screens
- streaming output presentation

## Transport Requirements

The event model should be transport-agnostic.

Suggested event types:

- `turn_started`
- `token_delta`
- `tool_requested`
- `tool_completed`
- `task_state_changed`
- `worker_state_changed`
- `turn_completed`

Transports:

- WebSocket for live streams
- HTTP for CRUD, config, history, and retrieval

Transport choices must not alter core runtime semantics.

## Execution Boundaries

Even inside one Spring Boot app, execution should be separated into:

- API request handling
- stream/session dispatch
- background task execution

## Persistence Requirements

Minimum persistence concepts:

- workspace
- runtime session
- client identity
- task owner
- transcript/event history

## Technology Direction

- Java 21
- Spring Boot 4
- Spring AI BOM 2.0 M4
- React 19
- Vite 6
- Postgres or H2-backed early persistence

## Spring AI Boundary

Spring AI is appropriate for:

- provider abstraction
- LLM access
- MCP support
- model configuration

Spring AI should not become the owner of:

- prompt design
- prompt precedence
- core instruction layering
- tool semantics
- runtime orchestration behavior
