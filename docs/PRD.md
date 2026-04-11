# PRD - enterprisewebagent

## 1. Product Overview

`enterprisewebagent` is a Java-first agent runtime product delivered through:

- a Spring-hosted runtime application
- a React web UI
- a Java CLI packaged with `jlink`

The product is intended to behave like its own agent runtime rather than a thin framework wrapper.

## 2. Problem Statement

Many AI-enabled applications stop at chat and model invocation. They do not provide:

- a consistent runtime across web and CLI
- explicit orchestration semantics
- clear task and session continuity
- structured tool execution
- transport-aware but transport-independent runtime behavior

`enterprisewebagent` aims to solve that by building a real runtime product with strong control over execution and interaction.

## 3. Product Goals

- create a shared runtime across web and CLI
- make Spring the application host, not the runtime owner
- use Spring AI as an LLM-agnostic provider/integration layer
- preserve existing agent-core behavior with minimal to no semantic drift
- support live streaming, task execution, and session continuity
- establish a foundation for more advanced orchestration later

## 4. Non-Goals

- full authentication and authorization in the first release
- separate worker service in the first release
- large admin/backoffice UI in the first release
- building the product as only a Spring AI demo application

## 5. Target Users

### Primary Users

- operators using the Java CLI
- users interacting through the React UI
- developers evolving the runtime and tool system

### Early Usage Assumption

The first phase is effectively single-operator or small-team usage, but internal runtime identities for sessions, tasks, and clients must still exist.

## 6. Product Requirements

### Must Have

- shared runtime core
- same core prompt structure and behavior as the current agent
- same core tool surface and execution semantics as the current agent
- same instruction hierarchy and runtime interaction style as the current agent
- same cache-sensitive prompt and runtime behavior where the current core relies on it
- Spring-hosted runtime shell
- React web UI
- Java CLI with `jlink`
- live streaming over WebSocket
- session model
- task model
- configurable provider/model layer

### Should Have

- resumable sessions
- background execution in the Spring app
- context-aware tool capability profiles
- basic observability for sessions and tasks

### Could Have Later

- advanced coordinator/worker specialization
- richer admin controls
- multi-user auth
- separate worker deployable

## 7. Success Criteria

The first implementation is successful if:

- core agent behavior remains effectively identical for prompts, tools, instructions, and interaction semantics
- the same runtime behavior is visible from web and CLI
- a session can be started, streamed, and completed from both surfaces
- a long-running task can be tracked and resumed
- Spring AI remains behind the provider boundary rather than leaking into product core design

## 8. Key Product Principle

> Spring AI is an LLM-agnostic provider and integration layer in the stack. `enterprisewebagent` still owns runtime behavior.

That principle should remain true across implementation decisions.

## 9. Core Parity Constraint

The existing agent core is the compatibility target for `enterprisewebagent`.

This means:

- no casual prompt rewrites
- no simplification of the core tool surface without explicit product intent
- no drift in instruction hierarchy or precedence
- no transport-specific reinterpretation of the runtime
- no replacement of current runtime semantics with framework defaults just because Spring AI exposes a convenient abstraction
