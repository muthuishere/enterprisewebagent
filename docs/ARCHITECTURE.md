# Architecture — enterprisewebagent

> Last updated: 2026-04-11 (post Phase 1 completion)

## Overview

`enterprisewebagent` is a Java-first agent runtime product. The runtime owns all behavioral logic — prompt assembly, tool execution, turn loops, worker orchestration, session/task state, and event publishing. Spring Boot hosts the runtime. Spring AI sits behind a provider adapter boundary. The CLI and frontend are thin clients consuming the same REST + WebSocket contracts.

```
┌──────────────────────────────────────────────────────────────┐
│                        Clients                               │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────────────┐ │
│  │  CLI      │   │  React UI    │   │  External (curl/SDK) │ │
│  │  Picocli  │   │  React 19    │   │  HTTP / WebSocket    │ │
│  └────┬──┬──┘   └─────┬──┬────┘   └──────────┬──┬───────┘ │
│       │  │             │  │                    │  │         │
└───────┼──┼─────────────┼──┼────────────────────┼──┼─────────┘
        │  │             │  │                    │  │
   HTTP │  │ WS     HTTP │  │ WS           HTTP │  │ WS
        │  │             │  │                    │  │
┌───────▼──▼─────────────▼──▼────────────────────▼──▼─────────┐
│                    Spring Boot Host                          │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                    app layer                           │  │
│  │  SessionController  TaskController  ConfigController   │  │
│  │  StreamingWebSocketHandler  WebSocketConfig            │  │
│  │  RuntimeConfig (bean wiring)                           │  │
│  └────────────────────┬───────────────────────────────────┘  │
│                       │ delegates to                         │
│  ┌────────────────────▼───────────────────────────────────┐  │
│  │                 runtime core                           │  │
│  │                                                        │  │
│  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌───────────┐  │  │
│  │  │ prompt   │ │  tools  │ │  query   │ │  agents   │  │  │
│  │  │ assembly │ │ registry│ │  engine  │ │  orchestr │  │  │
│  │  └─────────┘ └─────────┘ └──────────┘ └───────────┘  │  │
│  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌───────────┐  │  │
│  │  │ skills  │ │ events  │ │  memory  │ │  session  │  │  │
│  │  │ loader  │ │ pub/sub │ │  store   │ │  /task    │  │  │
│  │  └─────────┘ └─────────┘ └──────────┘ └───────────┘  │  │
│  │  ┌─────────┐                                          │  │
│  │  │provider │ ← Spring AI adapter boundary             │  │
│  │  │ adapter │                                          │  │
│  │  └─────────┘                                          │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## Runtime Core — Module Inventory

### prompt (`runtime.prompt`)

Owns prompt composition with 5-level precedence and cache-aware section assembly.

| Class | Role |
|-------|------|
| `PromptPrecedence` | Enum: OVERRIDE > COORDINATOR > CUSTOM_AGENT > USER_SYSTEM > DEFAULT |
| `PromptCatalog` | Enum mapping 12 named surfaces (prompt_001–012) with `cached` flag |
| `PromptSection` | Record: name, content, precedence, cached flag |
| `PromptContext` | Record carrying active precedence, session ID, role, custom instructions |
| `DefaultPromptAssembler` | Resolves sections by precedence, reads through cache for static sections |
| `InMemoryPromptSectionCache` | Thread-safe cache; `clear()` resets all, `clearDynamic()` only uncached |
| `PromptSectionRegistry` | Stores section content by catalog key |

**Precedence rules:**
- OVERRIDE bypasses all append and memory (used for system-level overrides)
- Other levels assemble: static prefix (001–005, cached) → dynamic boundary (006) → dynamic tail (012, uncached)

### tools (`runtime.tools`)

Owns the tool registry pipeline: register → deny-filter → mode-filter → sort by name.

| Class | Role |
|-------|------|
| `ToolDefinition` | Record: name, description, parameters schema, executor, modes |
| `ToolMode` | Enum: NORMAL, COORDINATOR, SIMPLE, WORKER |
| `DefaultToolRegistry` | Central registry with deny→mode→sort pipeline |
| `DenyRuleFilter` | Excludes tools by name (ConcurrentHashMap-backed) |
| `ModeToolFilter` | Filters tools by active mode |
| `ToolExecutor` | Interface for tool dispatch |
| `ToolResult` | Record: success flag, output, error |

**Built-in tools** (6):

| Tool | Purpose |
|------|---------|
| `ask_user` | Escalate to user for input |
| `file_read` | Read file contents |
| `file_edit` | Search-and-replace in files |
| `shell` | Execute shell commands (ProcessBuilder, timeout-bounded) |
| `task_stop` | Stop a running task |
| `worker_delegate` | Delegate work to a worker agent |

### query (`runtime.query`)

Owns the turn execution loop and transcript management.

| Class | Role |
|-------|------|
| `DefaultTurnEngine` | Core loop: prompt→model→parse→execute tools→re-prompt (max 10 iterations) |
| `StreamingTurnEngine` | Wraps DefaultTurnEngine with Flux streaming + TokenDeltaEvent per chunk |
| `ModelResponseParser` | Extracts tool calls from model response text |
| `Transcript` | Mutable conversation history (add, replaceAll, estimateTokens) |
| `TranscriptEntry` | Record: role, content, timestamp |
| `TurnRequest` / `TurnResult` | Request/response records for turn execution |
| `TranscriptCompactor` | Compaction strategies: CLEAR, COMPACT, TRUNCATE |
| `PromptBudgetCalculator` | Token estimation (chars/4) and budget checking |
| `CompactionStrategy` | Enum for compaction modes |

**Turn loop:**
```
User message
    ↓
Assemble prompt (precedence + cached sections + transcript)
    ↓
Call model provider
    ↓
Parse response → tool calls detected?
    ├─ Yes → execute tools → append results → re-call model (up to 10x)
    └─ No  → return final response as TurnResult
```

### agents (`runtime.agents`)

Owns coordinator/worker orchestration with depth limiting.

| Class | Role |
|-------|------|
| `AgentRole` | Enum: COORDINATOR, GENERAL_WORKER, EXPLORE_WORKER, PLAN_WORKER, VERIFY_WORKER |
| `AgentContext` | Carries session, depth counter, parent reference. `canNest()` checks maxDepth (default 3) |
| `AgentDefinition` | Record: role, scoped tool set, prompt catalog entry |
| `AgentDefinitionFactory` | Creates role-specific definitions with scoped tools |
| `DefaultWorkerOrchestrator` | Depth-limited delegation, sequential or parallel execution |
| `CoordinatorRuntime` | Parallel worker execution via virtual threads |
| `WorkerPromptResolver` | Maps AgentRole → PromptCatalog entry |
| `WorkerResult` | Record: role, success flag, output |

**Worker tool scoping:**
- GENERAL_WORKER: full tool set minus worker_delegate
- EXPLORE_WORKER: file_read + shell (read-only)
- PLAN_WORKER: file_read + shell (read-only)
- VERIFY_WORKER: file_read + shell (read-only)
- COORDINATOR: full tool set including worker_delegate

### skills (`runtime.skills`)

Loads markdown-based skills with frontmatter metadata.

| Class | Role |
|-------|------|
| `DefaultSkillLoader` | Loads from PROJECT → USER → MANAGED paths, deduplicates (first-seen wins) |
| `FrontmatterParser` | YAML frontmatter extraction from markdown files |
| `SkillFileScanner` | Walks directories for `.md` files matching skill conventions |
| `SkillDefinition` | Record: name, description, content, origin, enabled flag |
| `SkillPromptExpander` | Expands skill definitions into prompt sections |
| `SkillOrigin` | Enum: PROJECT, USER, MANAGED (trust boundary) |
| `SkillLoadContext` | Record carrying skill paths and filter settings |

### events (`runtime.events`)

Transport-agnostic event system.

```java
public sealed interface RuntimeEvent permits
    TurnStartedEvent,
    TokenDeltaEvent,
    ToolRequestedEvent,
    ToolCompletedEvent,
    TaskStateChangedEvent,
    WorkerStateChangedEvent,
    TurnCompletedEvent,
    TurnFailedEvent;
```

| Class | Role |
|-------|------|
| `InMemoryEventPublisher` | Synchronous dispatch to registered listeners |
| `AsyncEventPublisher` | Asynchronous dispatch via virtual threads |
| `EventBuffer` | Buffered event polling for clients |
| `EventSerializer` | JSON serialization for WebSocket transport |

### memory (`runtime.memory`)

Session-scoped memory and storage.

| Class | Role |
|-------|------|
| `InMemoryMemoryStore` | Key-value store for memory entries |
| `InMemorySessionMemory` | Session-scoped memory backed by MemoryStore |
| `MemoryEntry` | Record: key, value, timestamp |

### session (`runtime.session`)

Session lifecycle and compaction.

| Class | Role |
|-------|------|
| `Session` | Record: id, status, created timestamp, transcript |
| `SessionStatus` | Enum: ACTIVE, CLOSED, COMPACTED |
| `InMemorySessionManager` | ConcurrentHashMap-backed session CRUD |
| `SessionCompactionManager` | Orchestrates transcript compaction + cache clearing |

### tasks (`runtime.tasks`)

Task lifecycle tracking.

| Class | Role |
|-------|------|
| `TaskDefinition` | Record: id, sessionId, description, status, created |
| `TaskStatus` | Enum for task states |
| `InMemoryTaskManager` | ConcurrentHashMap-backed task CRUD |

### provider (`runtime.provider`)

Spring AI adapter boundary — the runtime never imports Spring AI directly.

| Class | Role |
|-------|------|
| `ModelProvider` | Interface: send prompt sections → get response string |
| `ModelProviderRegistry` | Registry of named providers |
| `DefaultModelProviderRegistry` | HashMap-backed, returns first registered if name is null |
| `PromptMapper` | Converts PromptSection list → Spring AI Message list |
| `SpringAiModelProvider` | Delegates to Spring AI ChatModel |
| `StubModelProvider` | Returns fixed response (used for tests and dev) |
| `ModelRequest` | Record: sections, temperature, maxTokens |

## App Layer

### REST API (`app.api`)

| Endpoint | Method | Controller |
|----------|--------|-----------|
| `POST /api/sessions` | Create session | SessionController |
| `GET /api/sessions/{id}` | Get session | SessionController |
| `POST /api/sessions/{id}/turns` | Execute turn | SessionController |
| `POST /api/sessions/{id}/close` | Close session | SessionController |
| `POST /api/tasks` | Create task | TaskController |
| `GET /api/tasks/{id}` | Get task | TaskController |
| `GET /api/tasks` | List tasks | TaskController |
| `PUT /api/tasks/{id}/status` | Update status | TaskController |
| `GET /api/config` | Runtime info | ConfigController |
| `GET /api/config/health` | Health check | ConfigController |

### WebSocket (`app.ws`)

- **Endpoint:** `/ws/stream`
- `StreamingWebSocketHandler` registers a `RuntimeEventListener` per connection
- Events serialize to JSON via `EventSerializer` and push to clients in real-time
- Listener lifecycle tied to connection open/close

### Bean Wiring (`app.config`)

`RuntimeConfig` wires all runtime components:
- `InMemoryPromptSectionCache` → `PromptSectionRegistry` → `DefaultPromptAssembler`
- `InMemoryEventPublisher`
- `DefaultToolRegistry` (with all built-ins registered via `BuiltInToolRegistrar`)
- `InMemorySessionManager`, `InMemoryTaskManager`
- `DefaultModelProviderRegistry` (with `StubModelProvider` as default)
- `DefaultTurnEngine` → `DefaultWorkerOrchestrator`

## CLI (`apps/cli`)

Picocli-based, no Spring dependency. Uses Java 21 `HttpClient` + Gson.

| Command | Subcommands | Purpose |
|---------|-------------|---------|
| `agent health` | — | Check server connectivity |
| `agent session` | `chat`, `close` | Interactive chat REPL, session lifecycle |
| `agent task` | `list`, `create`, `status` | Task management |
| `agent config` | — | Display runtime configuration |

`ServerClient` handles HTTP communication with error handling (connection refused → "Server not reachable"). `OutputFormatter` provides consistent CLI output with `✓`/`✗` prefixes.

## Frontend (`apps/frontend`)

React 19 + TypeScript + Vite 6, built with Bun.

| Module | Purpose |
|--------|---------|
| `lib/config.ts` | API base URL configuration |
| `lib/http.ts` | `apiRequest` + `endpoints` map |
| `lib/api.ts` | Typed API client (`sessionApi`, `taskApi`, `configApi`) |
| `lib/ws.ts` | WebSocket connection helper (`connectSession`) |
| `domain/session/types.ts` | Session, TurnResult, ChatMessage, TranscriptEntry types |
| `domain/session/useSession.ts` | Hook: session lifecycle, chat messages, WebSocket streaming |
| `domain/session/SessionPage.tsx` | Chat UI with create/close, message display, streaming, auto-scroll |
| `domain/tasks/types.ts` | TaskDefinition, TaskStatus types |
| `domain/tasks/TaskPanel.tsx` | Self-fetching task panel (polls every 5s) |

## Data Flows

### Turn Execution Flow

```
Client (CLI / React / curl)
  │
  │  POST /api/sessions/{id}/turns  { "message": "..." }
  ▼
SessionController
  │
  │  delegates to
  ▼
DefaultTurnEngine.executeTurn(turnRequest)
  │
  ├─ 1. PromptAssembler.assemble(context) → [PromptSection...]
  │     └─ checks cache for static sections
  │     └─ resolves dynamic sections fresh
  │
  ├─ 2. ModelProvider.call(sections) → response text
  │     └─ PromptMapper converts sections → Spring AI Messages
  │     └─ SpringAiModelProvider (or StubModelProvider) returns text
  │
  ├─ 3. ModelResponseParser.parse(response) → tool calls?
  │     ├─ Yes: ToolRegistry.resolveTools() → deny→mode→sort
  │     │       ToolExecutor.execute(toolCall) → ToolResult
  │     │       Append to transcript → go to step 2 (max 10x)
  │     └─ No:  return TurnResult
  │
  └─ Events published at each step:
       TurnStarted → TokenDelta* → ToolRequested → ToolCompleted → TurnCompleted
                                                                     │
                                                                     ▼
                                                            StreamingWebSocketHandler
                                                                     │
                                                                     ▼
                                                              Client (WebSocket)
```

### Worker Delegation Flow

```
Coordinator Turn
  │
  │  model calls worker_delegate tool
  ▼
WorkerDelegationTool.execute(role, task)
  │
  │  checks AgentContext.canNest() (maxDepth=3)
  ▼
DefaultWorkerOrchestrator.delegate(workerDef, task, childContext)
  │
  ├─ WorkerPromptResolver → role-specific PromptCatalog entry
  ├─ AgentDefinitionFactory → scoped tool set
  └─ TurnEngine.executeTurn() with worker context
       │
       └─ WorkerResult returned to coordinator's transcript
```

## Parity Status

Phase 1 faithfully reproduces the current agent core behavior. See [`PARITY_VERIFICATION.md`](PARITY_VERIFICATION.md).

| Area | Status |
|------|--------|
| Prompt precedence (5-level) | ✅ PASS |
| Prompt section caching (static/dynamic) | ✅ PASS |
| Prompt catalog (12 surfaces) | ✅ PASS |
| Tool ordering (deterministic) | ✅ PASS |
| Deny-rule filtering | ✅ PASS |
| Worker role prompts | ✅ PASS |
| Coordinator behavior | ✅ PASS |
| Session persistence | ⚠️ GAP — in-memory only |
| Streaming events | ✅ PASS |
| Ask-user escalation | ⚠️ PARTIAL — no dedicated event |
| Turn engine (tool-call loop) | ✅ PASS |
| Compaction/recovery | ✅ PASS |
| Skill loading | ✅ PASS |
| Transport (REST + WS) | ✅ PASS |

## What Remains — Phase 2

### Must-Do (gaps from parity)

1. **Durable session persistence** — implement `SessionManager` backed by H2/Postgres. The interface is ready; only `InMemorySessionManager` exists today. Covers sessions, transcripts, and task history.

2. **Ask-user event** — add an `AskUserRequestedEvent` to `RuntimeEvent` so WebSocket clients get a structured signal instead of parsing model text.

### Should-Do (production readiness)

3. **Auth + identity** — runtime boundaries are stable; auth was intentionally deferred. Needs: API key or JWT for REST/WS, client identity in session context.

4. **Real model provider** — swap `StubModelProvider` for a live LLM. `SpringAiModelProvider` exists but needs an API key and model configuration wired through `application.yml`.

5. **MCP tool integration** — Spring AI's MCP support can surface external tool servers. The `ToolRegistry` can accept dynamically registered tools.

6. **CLI packaging** — `jlink` packaging for the CLI binary (mentioned in TRD, not yet implemented).

7. **Frontend polish** — error boundaries, loading states, responsive layout, configuration screens, model selection UI.

8. **Observability** — structured logging, metrics (Micrometer), trace propagation through turn execution.

9. **Workspace concept** — the TRD mentions workspace as a persistence concept. Not yet modeled.

### Nice-to-Have (improvements — explicitly Phase 2)

10. **Prompt improvements** — any changes to prompt content, precedence rules, or caching strategy
11. **Additional tools** — web search, image generation, code interpreter
12. **Skill marketplace** — managed skill discovery and installation
13. **Multi-model routing** — route different agent roles to different models
14. **Rate limiting + quotas** — per-client usage tracking
