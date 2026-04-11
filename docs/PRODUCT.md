# PRODUCT

## Summary

`enterprisewebagent` is a Java-first agent runtime product with three primary surfaces:

- a Spring-hosted application runtime
- a React web UI
- a Java CLI packaged with `jlink`

The product should behave like its own runtime rather than a thin wrapper over an LLM framework.

## Product Idea

The product is intended to provide:

- a strong runtime identity
- multi-surface usage through web and CLI
- structured orchestration instead of simple chat-only flows
- tool-aware execution
- durable task and session continuity
- a provider-agnostic model layer underneath the runtime

## Positioning

The correct framing is:

- `enterprisewebagent` owns runtime behavior
- Spring hosts the runtime
- React exposes the web surface
- the Java CLI exposes the terminal surface
- Spring AI is used as an LLM-agnostic provider and integration layer

The wrong framing would be:

- a normal Spring AI application with some custom prompts and UI added on top

## Core Principle

Spring AI is in the stack, but it is not the product core.

The product core should own:

- prompt assembly
- turn and session execution
- tool registry and permissions
- coordinator and worker orchestration
- memory behavior
- task lifecycle
- runtime events

## Non-Negotiable Core Parity Requirement

`enterprisewebagent` must preserve the current agent core behavior as closely as possible. The current runtime is the behavioral source of truth.

That means the product should retain:

- the same core prompt structure and prompt intent
- the same tool model and tool-use expectations
- the same instruction layering and precedence behavior
- the same cache-aware and current-model-sensitive runtime behavior where the current agent already depends on it
- the same question-and-response interaction style as the current agent core
- the same orchestration semantics wherever the current core already defines them clearly

The migration goal is not to redesign the agent personality or simplify the runtime contract. The goal is to reproduce the current core faithfully inside a Spring-hosted, productized architecture with web and CLI surfaces.

## Parity Notes

- prompt rewrites should be treated as high-risk changes, not cleanup
- tool surface changes should be treated as compatibility changes, not refactors
- instruction precedence must remain stable across CLI and web usage
- transport or hosting changes are acceptable only if core runtime semantics remain intact

## Product Surfaces

### Web UI

The web UI is the main interactive product surface for:

- live sessions
- task monitoring
- model and runtime controls
- reviewing outputs and artifacts

### CLI

The CLI is the fast operator and developer surface for:

- local workflows
- automation entrypoints
- terminal-driven interaction
- debugging and scripted execution

### Hosted Runtime

The hosted runtime is the operational heart of the system:

- it runs the runtime core
- it persists state
- it streams events
- it manages background execution

## Why This Product Exists

The goal is not only to call models.

The goal is to provide a controllable agent runtime product with:

- stronger orchestration
- better session continuity
- repeatable task flows
- clearer tool and worker semantics
- more consistent behavior across web and CLI
