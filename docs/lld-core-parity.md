# LLD - Core Parity

## Purpose

This document turns the current core runtime into a concrete parity target for `enterprisewebagent`.

It exists for one reason:

- prompts, tools, instructions, cache behavior, and runtime semantics must be carried over with minimal to no behavioral drift

This is not a loose architectural inspiration document. It is a low-level parity contract.

## Copied Reference Artifact

The detailed behavior reference for the current core lives in:

- `reference-current-core-prompt-system.md`

The explicit prompt catalog lives in:

- `prompts/`

This LLD is the implementation-facing summary of what must be preserved.

## Parity Scope

The parity target includes:

- prompt precedence and prompt assembly behavior
- static versus dynamic system prompt sections
- cache-aware system prompt section handling
- tool registry composition and filtering
- tool permission filtering and capability shaping
- query and turn lifecycle semantics
- worker and coordinator orchestration behavior
- skill and prompt loading behavior
- session, task, and event semantics exposed to CLI and web clients

## Prompt Catalog

The parity-critical named prompts are documented as:

- `prompts/prompt_001_system_identity.md`
- `prompts/prompt_002_system_rules.md`
- `prompts/prompt_003_doing_tasks.md`
- `prompts/prompt_004_action_safety.md`
- `prompts/prompt_005_tool_usage.md`
- `prompts/prompt_006_session_guidance.md`
- `prompts/prompt_007_coordinator_mode.md`
- `prompts/prompt_008_worker_general.md`
- `prompts/prompt_009_worker_explore.md`
- `prompts/prompt_010_worker_plan.md`
- `prompts/prompt_011_worker_verify.md`
- `prompts/prompt_012_ask_user.md`

## Prompt Parity

### Prompt precedence

The effective prompt order in the current runtime is:

1. override system prompt
2. coordinator prompt when coordinator mode is active and no main-thread agent is selected
3. main-thread agent prompt
4. custom user-provided system prompt
5. default system prompt
6. append system prompt is added at the end unless the override path replaces everything

`enterprisewebagent` should preserve this precedence exactly.

### Prompt assembly shape

The default system prompt is divided into:

- static prefix
- dynamic boundary marker
- dynamic tail

The reference runtime uses a literal dynamic boundary marker to separate globally cacheable content from session-specific content. That behavior should be preserved, even if the implementation is JVM-native.

### Static prompt responsibilities

The static prompt includes the stable behavioral identity of the agent, including:

- high-level runtime identity
- system rules
- task execution guidance
- action safety guidance
- tool usage guidance
- tone and output efficiency behavior

These sections should remain stable and cache-friendly.

### Dynamic prompt responsibilities

The dynamic prompt tail includes sections such as:

- session guidance
- memory prompt
- model-specific suffixes and overrides
- environment information
- language preference
- output style prompt
- MCP instructions
- scratchpad instructions
- function-result-clearing behavior
- tool-result summarization reminders
- token budget guidance
- brief-mode guidance
- proactive or autonomous mode guidance

`enterprisewebagent` should keep the same conceptual split between stable and volatile prompt sections.

## Prompt Cache Parity

### Section cache behavior

The current runtime defines two prompt-section modes:

- memoized prompt sections that survive until clear or compact
- uncached prompt sections that recompute each turn and intentionally break cache stability

`enterprisewebagent` should implement an equivalent prompt-section cache with:

- named prompt sections
- cached versus uncached section modes
- explicit cache clearing on clear and compact boundaries
- stable ordering to avoid accidental cache invalidation

### Non-negotiable cache rules

- do not collapse all prompt generation into one giant string builder
- do not remove the stable-versus-dynamic boundary
- do not let transport or UI concerns mutate prompt bytes differently between CLI and web
- do not let framework defaults reorder prompt sections

## Tool Parity

### Base tool surface

The current runtime tool pool is built from a core set plus feature-gated additions. The base built-in surface includes categories such as:

- agent and worker tools
- task output and task stop tools
- shell and file system tools
- notebook editing
- web fetch and web search
- todo and task-management tools
- user question and brief/planning tools
- skill execution
- MCP resource listing and reading
- tool search
- LSP and workflow tools
- messaging and team or swarm tools
- monitoring, browser, cron, notification, and remote trigger tools when enabled

`enterprisewebagent` does not need the same source language, but it must preserve the same tool model:

- tool registration
- tool enablement gates
- deny-rule filtering
- context-aware tool availability
- deterministic tool ordering for cache stability
- combined built-in and MCP tool pool assembly

### Tool pool assembly rules

The current runtime behavior includes:

- built-in tools resolved first
- MCP tools filtered by deny rules
- deduplication by tool name
- stable sorting for prompt-cache stability
- mode-specific tool filtering for simple mode, coordinator mode, and REPL-style modes

This logic should be preserved semantically in `enterprisewebagent`.

## Instruction Parity

### Instruction layers

The current runtime behavior is not controlled by one file. It emerges from layered instruction sources:

- base system prompt
- dynamic prompt sections
- coordinator prompt
- custom agent prompts
- custom user system prompt
- append system prompt
- output style prompt
- memory prompt
- MCP instruction blocks
- skill markdown instructions
- project or user instruction files

`enterprisewebagent` should preserve this layered model rather than flattening everything into one framework prompt template.

### Instruction precedence rules

- override instructions replace all other top-level prompts
- coordinator instructions outrank default runtime prompt
- custom agent instructions outrank custom user system prompts
- append instructions extend the selected top-level prompt path
- dynamic prompt sections remain attached to the selected default runtime flow where applicable

## Query And Turn Parity

The current runtime query loop owns:

- turn execution
- transcript evolution
- tool call lifecycle
- tool result reinsertion
- compact and recovery behavior
- token budget handling
- streaming event flow
- command queue and interruption behavior

`enterprisewebagent` should preserve these semantics in a JVM-native runtime module. Spring should host that runtime, not redefine it.

## Worker And Coordinator Parity

The current runtime supports:

- coordinator-specific prompt behavior
- agent tool driven worker execution
- main-thread and subagent distinctions
- inherited or overridden prompt context for workers
- cache-safe worker execution assumptions

This means `enterprisewebagent` should model workers as runtime entities, not as ad hoc asynchronous service calls.

## Skill And Prompt Loading Parity

The current runtime can load markdown-driven skill prompts from multiple origins, including:

- project
- user
- managed
- plugin
- MCP-related sources

That means the future `enterprisewebagent` runtime should preserve:

- markdown-backed prompt or skill loading
- frontmatter-driven configuration
- deterministic load order
- deduplication behavior
- trust boundary awareness for remote sources

## JVM Implementation Mapping

The target module split for parity should be:

- `runtime-prompt`
- `runtime-tools`
- `runtime-query`
- `runtime-agents`
- `runtime-skills`
- `runtime-memory`
- `runtime-events`
- `runtime-provider`

Suggested responsibility mapping:

- `runtime-prompt` mirrors prompt precedence, section assembly, and prompt cache logic
- `runtime-tools` mirrors tool registration, enablement, filtering, and pool assembly
- `runtime-query` mirrors turn execution, streaming, compaction hooks, and recovery behavior
- `runtime-agents` mirrors coordinator mode, worker orchestration, and agent definitions
- `runtime-skills` mirrors markdown skill loading and instruction expansion
- `runtime-provider` uses Spring AI as the provider and MCP integration layer without taking over runtime semantics

## Spring AI Boundary

Spring AI is acceptable for:

- provider abstraction
- model execution plumbing
- MCP integration support
- model configuration

Spring AI must not silently replace:

- prompt precedence
- instruction layering
- tool semantics
- query loop semantics
- worker orchestration behavior
- cache-sensitive prompt assembly

## Implementation Rule

`enterprisewebagent` should be considered parity-ready only when these conditions are true:

- copied current-core reference docs are present in the repo
- runtime parity rules are encoded in product and technical docs
- the JVM runtime design mirrors prompt, tool, instruction, and turn semantics from the current core

At the current stage, the first two are satisfied. The third remains an implementation task.
