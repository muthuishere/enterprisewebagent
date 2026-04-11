# Current Core Runtime Behavior Reference

## Purpose

This document describes the current core only in terms of behavior.

It does not reference implementation files. It exists to capture what the agent does, how it thinks, how it composes prompts, how it chooses tools, and how it runs workers.

For prompt-level parity, use the prompt catalog under `docs/prompts/`.

## Runtime Shape

The current core behaves like a layered agent runtime with six major responsibilities:

- startup and environment preparation
- prompt composition and precedence resolution
- turn execution and streaming
- tool registration and tool filtering
- worker and coordinator orchestration
- skill and instruction expansion from markdown-driven surfaces

The key property is consistency. The same runtime identity is preserved while the active prompt, enabled tools, worker mode, and dynamic guidance shift based on context.

## Startup Behavior

The current core starts cheaply and branches into the correct operating mode early.

It supports a normal interactive mode, prompt-dump style debugging, specialized runtime modes, remote or background execution paths, and other fast-path entry flows without loading unnecessary subsystems.

After routing to the correct execution path, it performs shared initialization once and reuses that initialization across the session lifetime.

Shared initialization prepares:

- configuration
- settings and policy
- telemetry and analytics hooks
- account and session state
- environment and repository context
- network and transport setup
- scratch and temporary working areas
- cleanup and shutdown behavior

## Prompt System Overview

The current core prompt system is layered. It does not rely on one giant static prompt.

Instead, it combines:

- a stable base runtime identity
- dynamic session guidance
- optional memory and output-style additions
- optional coordinator or custom-agent overrides
- optional append-only extra instructions

The final result is one effective prompt stack for the current turn.

## Prompt Precedence

The effective top-level prompt is chosen in this order:

1. full override prompt
2. coordinator-mode prompt
3. active custom-agent prompt
4. user-specified system prompt
5. default runtime prompt

After that selection, append-only extra instructions are added unless a full override replaces everything.

This precedence is one of the most important parity rules and must remain stable.

## Prompt Structure

The default runtime prompt is split into:

- a static cache-friendly prefix
- a boundary between stable and volatile content
- a dynamic session-aware tail

The static area holds the runtime identity and broad behavioral rules.

The dynamic area holds context-sensitive guidance such as:

- session guidance
- memory
- environment information
- output style
- model-specific behavior
- MCP guidance
- scratchpad behavior
- token-budget and summarization reminders
- brief-mode or autonomous-mode additions

## Prompt Catalog

The following prompt documents represent the named prompt surfaces that should be mirrored in `enterprisewebagent`:

- [prompts/prompt_001_system_identity.md](./prompts/prompt_001_system_identity.md)
- [prompts/prompt_002_system_rules.md](./prompts/prompt_002_system_rules.md)
- [prompts/prompt_003_doing_tasks.md](./prompts/prompt_003_doing_tasks.md)
- [prompts/prompt_004_action_safety.md](./prompts/prompt_004_action_safety.md)
- [prompts/prompt_005_tool_usage.md](./prompts/prompt_005_tool_usage.md)
- [prompts/prompt_006_session_guidance.md](./prompts/prompt_006_session_guidance.md)
- [prompts/prompt_007_coordinator_mode.md](./prompts/prompt_007_coordinator_mode.md)
- [prompts/prompt_008_worker_general.md](./prompts/prompt_008_worker_general.md)
- [prompts/prompt_009_worker_explore.md](./prompts/prompt_009_worker_explore.md)
- [prompts/prompt_010_worker_plan.md](./prompts/prompt_010_worker_plan.md)
- [prompts/prompt_011_worker_verify.md](./prompts/prompt_011_worker_verify.md)
- [prompts/prompt_012_ask_user.md](./prompts/prompt_012_ask_user.md)

## Base Runtime Identity

The current core presents itself as an interactive engineering agent.

Its behavioral identity includes:

- helping users perform real work rather than only discussing it
- preferring action over abstract explanation when the task is concrete
- using tools to inspect reality before making claims
- treating the user as a collaborator
- balancing initiative with care on risky operations

The runtime identity is not generic “assistant” behavior. It is a pragmatic operator-style identity.

## System Rules Behavior

The current core assumes:

- normal text responses are user-visible output
- tools run under explicit permission policy
- system reminders and tags can appear in tool results and user messages
- prompt-injection risk should be noticed and surfaced
- context is maintained through summarization and compaction rather than a short fixed chat window

This gives the agent a durable, operational interaction model instead of a simple conversational one.

## Task-Execution Behavior

The current core strongly prefers grounded execution.

Its task behavior includes:

- reading relevant material before modifying it
- avoiding speculative abstractions
- avoiding unsolicited cleanup or product reshaping
- verifying work before claiming success where possible
- reporting outcomes faithfully
- escalating only when genuinely blocked

It behaves like a senior engineer under execution pressure, not a generic explainer.

## Action-Safety Behavior

The current core distinguishes between:

- low-risk local and reversible actions
- high-risk, destructive, shared-state, or hard-to-reverse actions

For risky actions, the default behavior is transparent caution and confirmation unless durable instructions authorize broader autonomy.

This caution is behavioral, not only permission-driven.

## Tool-Usage Behavior

The current core prefers dedicated tools when they provide clearer semantics than shell commands.

Its tool behavior includes:

- selecting the most appropriate tool for the task
- filtering tools by mode and policy
- preserving deterministic tool ordering
- merging built-in and external tool surfaces carefully
- avoiding duplicate or denied tools
- using specialized search, file, task, or worker tools where appropriate

The visible tool list is not the whole story. The runtime also shapes tool availability based on session mode.

## Query And Turn Behavior

The current core executes a turn as a structured loop.

That loop handles:

- message preparation
- effective prompt use
- streaming model output
- tool-call issuance
- tool-result reinsertion
- interruption and continuation
- recovery behavior
- budget and compaction handling
- transcript updates

A turn is not just one request-response exchange. It is an execution loop with state.

## Prompt Cache Behavior

The current core treats prompt stability as an optimization boundary.

It distinguishes between:

- sections that should stay stable and reusable until a clear or compact event
- sections that must be recomputed because they are intentionally volatile

Prompt caching is therefore linked to:

- section naming
- stable ordering
- explicit boundaries
- careful avoidance of accidental drift between sessions and clients

This matters because parity is not only about words. It is also about when and why prompt bytes change.

## Coordinator Behavior

The current core can switch into a coordinator identity.

In this mode, the main agent behaves like an orchestrator rather than an ordinary single-thread worker. It can:

- manage other workers
- delegate exploration or planning
- coordinate multi-step execution
- keep the main thread focused on orchestration

Coordinator mode is not just a feature flag. It changes the top-level runtime role.

## Worker Behavior

Workers are treated as real runtime entities with their own prompt identity, context, and scope.

The current core supports multiple worker roles, including:

- a general worker
- an exploration-focused worker
- a planning-focused worker
- a verification-focused worker

Worker execution preserves:

- parent-child coordination semantics
- prompt inheritance or override behavior
- controlled tool access
- worker-specific reporting expectations

## Skill And Instruction Expansion

The current core supports markdown-driven instruction expansion from multiple origins.

These instruction surfaces can add behavior without changing the runtime core directly.

This includes:

- local project instructions
- user-level instructions
- managed instructions
- plugin-provided instructions
- externally provided prompt or skill surfaces

These are merged through a controlled load and precedence model.

## Memory And Session Guidance

The current core can attach memory and session-aware guidance to the active prompt.

This can influence:

- what context is surfaced for the current turn
- how the runtime behaves across long sessions
- how ongoing work is summarized or resumed
- how the agent stays aligned with durable project guidance

Memory is therefore a prompt-affecting runtime concern, not just persistence.

## Compact And Recovery Behavior

The current core anticipates long sessions and prompt-size pressure.

It supports:

- compaction
- summarization
- prompt-too-long recovery
- output-budget recovery
- state continuation after interruptions

This is part of the runtime contract and should remain present in parity planning.

## Product Translation Rule

`enterprisewebagent` should copy this behavior, not merely imitate the vibe of it.

That means:

- same prompt precedence
- same layered instruction model
- same stable-versus-dynamic prompt split
- same tool-selection philosophy
- same worker and coordinator semantics
- same turn-loop mentality

The implementation language can change. The runtime behavior should not drift casually.
