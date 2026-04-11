# Implementation Handoff

## Purpose

This document is for another LLM that will continue the build of `enterprisewebagent`.

It should be treated as a step-by-step execution guide, not a vision document.

The primary rule is simple:

- preserve the current core behavior as closely as possible while rebuilding it as a Java-first runtime with a Spring-hosted backend, React web UI, and Java CLI

## Non-Negotiable Constraints

The implementing LLM must not drift from these constraints:

- do not redesign the runtime personality
- do not simplify prompt precedence
- do not flatten instruction layering into one generic framework prompt
- do not casually rename, merge, or remove prompt surfaces
- do not change tool semantics unless explicitly requested
- do not let web and CLI become two different runtimes
- do not let Spring AI become the product core

The existing behavior reference is documented in:

- `reference-current-core-prompt-system.md`
- `lld-core-parity.md`
- `prompts/`

## Product Shape To Build

Build one product with three user-facing surfaces:

- Spring-hosted runtime application
- React web UI
- Java CLI packaged with `jlink`

Build one shared runtime core behind those surfaces.

The runtime core should own:

- prompt composition
- prompt precedence
- prompt cache behavior
- tool registry and tool filtering
- turn execution
- worker and coordinator behavior
- session and task state
- runtime event emission

Spring AI should be used only as:

- provider abstraction
- model request plumbing
- MCP support layer
- model configuration support

## Expected Directory Direction

The next implementation should move the repository toward this structure:

- `src/main/java/.../runtime/prompt`
- `src/main/java/.../runtime/tools`
- `src/main/java/.../runtime/query`
- `src/main/java/.../runtime/agents`
- `src/main/java/.../runtime/skills`
- `src/main/java/.../runtime/memory`
- `src/main/java/.../runtime/events`
- `src/main/java/.../runtime/provider`
- `src/main/java/.../app/api`
- `src/main/java/.../app/ws`
- `src/main/java/.../app/tasks`
- `frontend/src/...`

The exact package names can follow repo conventions, but the separation of responsibility should remain.

## Implementation Order

Follow this order. Do not jump to UI polish or advanced features before the runtime skeleton is stable.

### Step 1 - Freeze the behavioral contract

Before writing runtime code, confirm the implementation plan is aligned with:

- `PRODUCT.md`
- `PRD.md`
- `trd-enterprisewebagent.md`
- `lld-core-parity.md`
- `reference-current-core-prompt-system.md`
- `prompts/`

Output of this step:

- one short implementation note confirming that prompt parity, tool parity, and runtime parity are the target

Acceptance criteria:

- no ambiguity remains about whether the product is “Spring AI app first” or “runtime first”
- the implementing LLM explicitly acknowledges that runtime behavior is the core deliverable

### Step 2 - Create runtime module skeletons

Create the Java package skeleton for:

- runtime prompt
- runtime tools
- runtime query
- runtime agents
- runtime skills
- runtime memory
- runtime events
- runtime provider

At this stage, only create the minimum class and interface skeletons needed to make the architecture concrete.

Output of this step:

- package layout
- placeholder interfaces and types for the runtime layers

Acceptance criteria:

- the packages exist
- the module boundaries clearly separate prompt, tools, query loop, agents, and provider integration

### Step 3 - Implement the prompt model

Build the prompt system before building orchestration.

Implement:

- named prompt surface representation
- top-level prompt precedence
- static prompt prefix support
- dynamic prompt tail support
- append-only prompt support
- prompt override support
- coordinator prompt path
- worker prompt path
- prompt catalog mapping to the named prompts in `docs/prompts`

Output of this step:

- a runtime prompt builder that can assemble the effective prompt stack for a turn

Acceptance criteria:

- the runtime can resolve the effective prompt in the documented order
- the named prompt surfaces are represented explicitly
- CLI and web paths use the same prompt builder

### Step 4 - Implement prompt-section caching

Add prompt-section caching before the query loop grows around unstable behavior.

Implement:

- named prompt sections
- cached sections
- uncached sections
- clearable cache state
- explicit boundary between stable prompt content and dynamic prompt content

Output of this step:

- prompt cache service and prompt section registry

Acceptance criteria:

- cached sections persist until clear or compact style reset
- uncached sections can be recomputed without collapsing the entire model into one volatile prompt string
- prompt ordering remains deterministic

### Step 5 - Implement the runtime event model

Define runtime events before wiring transports.

Implement events for:

- turn started
- token delta
- tool requested
- tool completed
- task state changed
- worker state changed
- turn completed
- turn failed

Output of this step:

- transport-agnostic runtime event types

Acceptance criteria:

- no event type depends directly on HTTP or WebSocket classes
- the same event model can be consumed by CLI and web

### Step 6 - Implement tool contracts

Create the common tool model.

Implement:

- tool identity
- tool description
- tool availability rules
- tool invocation contract
- deny-rule filtering
- mode-aware tool visibility
- built-in tool pool assembly
- MCP tool pool merging rules

Do not implement every tool body yet. Start with the contract and registry.

Output of this step:

- runtime tool registry
- runtime tool filtering layer

Acceptance criteria:

- built-in and external tools can be merged deterministically
- denied tools are filtered before runtime exposure
- tool ordering is stable

### Step 7 - Implement the minimum tool set

Implement the first usable tool set needed to prove the runtime:

- ask-user style tool
- file read
- file edit or write
- shell execution
- task stop
- worker delegation tool

If MCP integration is simple to stub, add the MCP resource listing and reading contract early, but keep it secondary to the core tools above.

Output of this step:

- a small but working operator-grade tool surface

Acceptance criteria:

- a prompt can lead to a tool call
- a tool result can be returned into the runtime loop
- the tool system behaves the same regardless of client surface

### Step 8 - Implement the query loop

Now build the turn engine.

Implement:

- message preparation
- effective prompt attachment
- streaming model response handling
- tool-call detection and execution
- tool-result reinsertion
- turn completion logic
- interruption and continuation support
- transcript accumulation

Do not start with every edge case. Start with the main loop, but keep the architecture ready for compaction and recovery behavior.

Output of this step:

- runtime query engine

Acceptance criteria:

- one end-to-end turn with a tool call works
- the runtime can emit streaming events during execution
- prompt builder, tool registry, and query loop operate together

### Step 9 - Implement worker and coordinator semantics

Add orchestration only after the single-agent loop works.

Implement:

- coordinator runtime mode
- worker definitions
- worker prompt selection
- parent-child runtime context
- worker event reporting
- bounded worker execution

Start with these worker roles:

- general worker
- explore worker
- plan worker
- verify worker

Use the named prompt docs in `prompts/` as the behavioral contract.

Output of this step:

- worker-capable runtime

Acceptance criteria:

- the main runtime can delegate to a worker
- the worker uses a different role-specific prompt path
- the parent runtime receives a structured worker result

### Step 10 - Implement session and task state

Add durable state after the runtime loop and workers exist.

Implement:

- session identity
- task identity
- transcript persistence
- task status
- worker-task linkage
- resumable turn history

No authentication is required right now, but runtime identities are still required.

Output of this step:

- session and task persistence model

Acceptance criteria:

- a session can be resumed
- a task can be tracked independently of a single network connection
- runtime state does not live only in memory

### Step 11 - Implement skill loading

Add markdown-driven skill expansion.

Implement:

- prompt or skill definitions loaded from markdown
- frontmatter-aware configuration
- deterministic load order
- deduplication
- clear origin tracking for trust boundaries

Do not make this overly fancy first. Make it stable and predictable.

Output of this step:

- runtime skill loader

Acceptance criteria:

- skills can add prompt-driven behavior without changing the runtime core
- load order and duplicate handling are deterministic

### Step 12 - Implement provider integration through Spring AI

Only now connect the runtime to Spring AI.

Implement:

- model execution adapter
- request and response mapping
- streaming adapter
- model selection support
- MCP-facing integration where needed

Do not let Spring AI decide prompt design or orchestration behavior.

Output of this step:

- provider adapter layer

Acceptance criteria:

- the runtime can execute turns through Spring AI
- Spring AI is behind the runtime boundary rather than inside prompt logic

### Step 13 - Expose the runtime through the Spring-hosted app

Add transport after the runtime works locally.

Implement:

- HTTP endpoints for session and task operations
- WebSocket streaming for live runtime events
- background execution inside the same Spring app

Keep execution lanes separate for:

- request handling
- streaming dispatch
- background task execution

Output of this step:

- Spring-hosted runtime application

Acceptance criteria:

- runtime events stream over WebSocket
- sessions and tasks can be managed through HTTP
- background work does not block request responsiveness

### Step 14 - Build the Java CLI

Build the Java CLI as a first-class product surface.

Implement:

- interactive session commands
- streaming terminal rendering
- task monitoring commands
- model and runtime selection options
- local operator workflow commands

Package the CLI with `jlink` only after the CLI behavior is stable.

Output of this step:

- usable terminal client

Acceptance criteria:

- CLI uses the same runtime contracts as the hosted product
- CLI behavior is not a separate runtime fork

### Step 15 - Build the React web UI

Build the React web UI after the runtime and transports are stable.

Implement:

- session view
- live streaming view
- task status view
- model and runtime controls
- output and artifact display

Output of this step:

- usable web client

Acceptance criteria:

- the web UI reflects the same runtime semantics as the CLI
- the web UI is not only a chat box; it exposes task and runtime state

### Step 16 - Add compaction and recovery behavior

After the core product loop works, add the long-session resilience behaviors.

Implement:

- clear-style prompt cache reset
- compact-style prompt cache reset
- transcript compaction support
- prompt-too-long recovery path
- output-budget recovery path
- resumable turn continuation where practical

Output of this step:

- long-session runtime resilience

Acceptance criteria:

- prompt stability and recovery behavior match the documented parity rules
- long sessions do not degrade into broken prompt assembly

### Step 17 - Verify parity explicitly

Do not end with “it roughly works.”

Create a parity checklist and verify:

- prompt precedence
- prompt section stability
- tool ordering
- deny-rule filtering
- worker role prompts
- coordinator behavior
- session persistence
- streaming events
- ask-user escalation behavior

Output of this step:

- parity verification document

Acceptance criteria:

- each parity area has an explicit pass or gap note
- unresolved gaps are documented rather than hidden

## Prompt Mapping To Implement

These prompt docs must be represented explicitly in the runtime design:

- `prompt_001_system_identity.md`
- `prompt_002_system_rules.md`
- `prompt_003_doing_tasks.md`
- `prompt_004_action_safety.md`
- `prompt_005_tool_usage.md`
- `prompt_006_session_guidance.md`
- `prompt_007_coordinator_mode.md`
- `prompt_008_worker_general.md`
- `prompt_009_worker_explore.md`
- `prompt_010_worker_plan.md`
- `prompt_011_worker_verify.md`
- `prompt_012_ask_user.md`

Do not collapse these into unnamed internal text blobs.

## What Not To Do

The implementing LLM should avoid these common failure modes:

- building the web API first and inventing runtime semantics later
- letting Spring AI own prompt orchestration
- implementing different prompt paths for CLI and web
- skipping prompt-section caching and promising to add it later
- treating workers as generic async jobs with no role identity
- flattening markdown-driven skills into hard-coded logic
- changing prompt surfaces because they seem redundant
- claiming parity without checking actual precedence and event behavior

## Minimum Viable Milestone Sequence

Use these milestones to keep delivery grounded:

1. runtime package skeleton exists
2. prompt builder works
3. prompt-section cache works
4. tool registry works
5. one tool-enabled query loop works
6. worker delegation works
7. session and task state work
8. Spring-hosted runtime works
9. Java CLI works
10. React UI works
11. compaction and recovery work
12. parity verification is written

## Handoff Completion Definition

This handoff is complete when the next LLM can start implementation without asking:

- what the product is
- what the runtime owns
- what prompt surfaces exist
- what order to build things in
- what constraints cannot be violated

If there is uncertainty, the next LLM should resolve it in favor of parity with the documented current-core behavior rather than inventing a simpler architecture.
