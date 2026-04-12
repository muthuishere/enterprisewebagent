# Architecture — enterprisewebagent

> Last updated: 2026-04-12 (post Phase 4 — Functional Parity)

## Overview

`enterprisewebagent` is a Java-first agent runtime product. The runtime owns all behavioral logic — prompt assembly, tool execution, turn loops, worker orchestration, session/task state, event publishing, planning, permissions, cost tracking, and slash commands. Spring Boot hosts the runtime. Spring AI sits behind a provider adapter boundary. The CLI and frontend are thin clients consuming the same REST + WebSocket contracts.

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
│  │  CostController  PermissionController                 │  │
│  │  StreamingWebSocketHandler  WebSocketConfig            │  │
│  │  RuntimeConfig (bean wiring)                           │  │
│  └────────────────────┬───────────────────────────────────┘  │
│                       │ delegates to                         │
│  ┌────────────────────▼───────────────────────────────────┐  │
│  │                 runtime core                           │  │
│  │                                                        │  │
│  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌───────────┐  │  │
│  │  │ prompt   │ │  tools  │ │  query   │ │  agents   │  │  │
│  │  │ assembly │ │ 23 tools│ │  engine  │ │  orchestr │  │  │
│  │  └─────────┘ └─────────┘ └──────────┘ └───────────┘  │  │
│  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌───────────┐  │  │
│  │  │ skills  │ │ events  │ │  memory  │ │  session  │  │  │
│  │  │ loader  │ │ pub/sub │ │ +MEMORY  │ │  /task    │  │  │
│  │  └─────────┘ └─────────┘ └──────────┘ └───────────┘  │  │
│  │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌───────────┐  │  │
│  │  │provider │ │commands │ │ planning │ │permissions│  │  │
│  │  │ 5 LLMs  │ │ 15 cmds │ │ +think  │ │ +cost     │  │  │
│  │  └─────────┘ └─────────┘ └──────────┘ └───────────┘  │  │
│  │  ┌─────────┐ ┌─────────┐                              │  │
│  │  │  teams  │ │  hooks  │                              │  │
│  │  │ collab  │ │ system  │                              │  │
│  │  └─────────┘ └─────────┘                              │  │
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

### tools (`runtime.tools`)

Owns the tool registry pipeline: register → deny-filter → mode-filter → permission-filter → sort by name.

| Class | Role |
|-------|------|
| `ToolDefinition` | Record: name, description, parameters schema, builtIn flag |
| `ToolMode` | Enum: NORMAL, COORDINATOR, SIMPLE, WORKER |
| `DefaultToolRegistry` | Central registry with extensible filter chain |
| `DenyRuleFilter` | Excludes tools by name |
| `ModeToolFilter` | Filters tools by active mode |
| `ToolExecutor` | Interface for tool dispatch |
| `ToolResult` | Record: name, output, success flag |

**Built-in tools (23):**

| Tool | Purpose | Registrar |
|------|---------|-----------|
| `ask_user` | Escalate to user for input | BuiltInToolRegistrar |
| `file_read` | Read file contents | BuiltInToolRegistrar |
| `file_edit` | Search-and-replace in files | BuiltInToolRegistrar |
| `file_write` | Write new files (no-overwrite safety) | BuiltInToolRegistrar |
| `shell` | Execute shell commands (timeout-bounded) | BuiltInToolRegistrar |
| `task_stop` | Stop a running task | BuiltInToolRegistrar |
| `worker_delegate` | Delegate work to a worker agent | BuiltInToolRegistrar |
| `glob` | File pattern matching (skip hidden, 1000 limit) | BuiltInToolRegistrar |
| `grep` | Regex file search with include filter | BuiltInToolRegistrar |
| `web_search` | DuckDuckGo search with result parsing | BuiltInToolRegistrar |
| `web_fetch` | HTTP GET with HTML stripping | BuiltInToolRegistrar |
| `sleep` | Pause execution (max 300s) | BuiltInToolRegistrar |
| `agent` | Spawn custom agents (foreground/background) | AgentTeamToolRegistrar |
| `team_create` | Create agent teams | AgentTeamToolRegistrar |
| `team_delete` | Delete teams | AgentTeamToolRegistrar |
| `send_message` | Inter-agent messaging | AgentTeamToolRegistrar |
| `enter_plan_mode` | Activate plan mode | PlanningToolRegistrar |
| `exit_plan_mode` | Deactivate plan mode | PlanningToolRegistrar |
| `memory` | Session memory management (show/add/clear/save) | MemoryHookRegistrar |
| `git_diff` | Git diff with staged/commit options | GitToolRegistrar |
| `git_commit` | Git commit with file staging | GitToolRegistrar |
| `git_branch` | Git branch CRUD | GitToolRegistrar |
| `git_log` | Git log with formatting | GitToolRegistrar |
| `git_status` | Git status (porcelain) | GitToolRegistrar |

**MCP bridge:** `McpToolBridge` discovers and registers tools from external MCP servers with namespace prefixing.

### commands (`runtime.commands`)

Slash command system — inputs starting with `/` are intercepted before reaching the model.

| Class | Role |
|-------|------|
| `Command` | Interface: name, description, usage, execute |
| `CommandResult` | Record: output, success, suppressTurn flag |
| `CommandContext` | Record: sessionId, rawInput, args, manager references |
| `CommandRegistry` | Thread-safe command registry with sorted listing |
| `CommandDispatcher` | Parses `/command args`, dispatches, handles errors |

**15 built-in commands:**

| Command | Purpose |
|---------|---------|
| `/help [cmd]` | List all or detail one command |
| `/status` | Session info + active provider |
| `/clear` | Reset conversation history |
| `/resume [id]` | Resume a closed session |
| `/cost` | Token usage and cost summary |
| `/memory [show\|add\|clear]` | View/manage session memory |
| `/skills [list\|enable\|disable]` | Manage loaded skills |
| `/config [key] [value]` | View/set configuration |
| `/plan [on\|off]` | Toggle plan mode |
| `/review [path]` | Request code review |
| `/diff [path]` | Show git diff |
| `/commit [message]` | Git commit |
| `/version` | Version + runtime info |
| `/session [list\|tag\|export]` | Session management |
| `/model [name]` | Show/switch model |

### query (`runtime.query`)

Owns the turn execution loop, thinking support, and transcript management.

| Class | Role |
|-------|------|
| `DefaultTurnEngine` | Core loop: prompt→model→parse→permission check→execute tools→re-prompt (max 10 iterations) |
| `ModelResponseParser` | Extracts tool calls from model response text |
| `Transcript` | Mutable conversation history |
| `TranscriptEntry` | Record: role, content, timestamp |
| `TurnRequest` / `TurnResult` | Request/response records for turn execution |
| `TranscriptCompactor` | Compaction strategies: CLEAR, COMPACT, TRUNCATE |
| `PromptBudgetCalculator` | Token estimation and budget checking |
| `ThinkingConfig` | Record: mode (DISABLED/ENABLED/ADAPTIVE), budgetTokens |
| `ThinkingPromptInjector` | Injects thinking instructions (adaptive for complex requests) |
| `ThinkingResponseParser` | Extracts `<thinking>` blocks from model responses |

**Turn loop (enhanced):**
```
User message
    ↓
Command check (CommandDispatcher) — if /command, dispatch and return
    ↓
Assemble prompt (precedence + cached sections + plan context + memory + transcript)
    ↓
Inject thinking instructions (if enabled/adaptive)
    ↓
Call model provider (with per-request routing)
    ↓
Parse thinking blocks → publish ThinkingEvent
    ↓
Parse response → tool calls detected?
    ├─ Yes → permission check → execute tools → record cost → re-call model (up to 10x)
    └─ No  → record cost → return TurnResult
```

### planning (`runtime.planning`)

Plan mode — structured planning before execution.

| Class | Role |
|-------|------|
| `PlanMode` | Enum: OFF, PLANNING, REVIEWING, EXECUTING |
| `PlanStep` | Record: id, description, status, result |
| `PlanStepStatus` | Enum: PENDING, APPROVED, REJECTED, IN_PROGRESS, COMPLETED, FAILED |
| `Plan` | Record: sessionId, goal, steps, mode, created |
| `PlanManager` / `InMemoryPlanManager` | Plan CRUD + step management |
| `PlanModeToolFilter` | Restricts tools per mode (PLANNING → read-only, REVIEWING → ask_user only) |
| `PlanModePromptContributor` | Injects plan context into prompts |

### permissions (`runtime.permissions`)

Permission system with auto-approval and bash safety analysis.

| Class | Role |
|-------|------|
| `PermissionMode` | Enum: UNRESTRICTED, APPROVE_ALL, AUTO_APPROVE, LOCKED |
| `PermissionRule` | Record: id, type (ALLOW/DENY/ASK), scope (TOOL/BASH/FILE), pattern |
| `BashSafetyAnalyzer` | Classifies commands: SAFE, MODERATE, DANGEROUS |
| `FilePathChecker` | Blocks access outside project root + sensitive paths |
| `PermissionEvaluator` | Mode-based evaluation combining all checkers |
| `PermissionDecision` | Record: allowed, reason, requiresApproval |
| `DenialTracker` | Audit trail for denied operations |
| `PermissionToolFilter` | Integrates with tool registry filter chain |

**Dangerous command patterns:** `rm -rf`, `DROP TABLE`, `git push --force`, `git reset --hard`, `chmod 777`, `shutdown`, pipe to `eval`/`sh`

### cost (`runtime.cost`)

USD cost tracking per model, per turn, per session.

| Class | Role |
|-------|------|
| `ModelPricing` | Static price catalog (input/output per 1M tokens) for all models |
| `CostCalculator` | Computes USD cost per turn |
| `SessionCostTracker` | Aggregates costs per session |
| `TurnCost` | Record: model, inputTokens, outputTokens, totalUsd, apiDuration |
| `SessionCostSummary` | Record: totalTurns, totalTokens, totalUsd |

**Pricing:** GPT-4.1 ($2/$8), Claude Sonnet ($3/$15), Claude Opus ($15/$75), Ollama/Copilot/Codex ($0 — local/subscription)

### agents (`runtime.agents`)

Coordinator/worker orchestration with depth limiting + custom agent definitions.

| Class | Role |
|-------|------|
| `AgentRole` | Enum: COORDINATOR, GENERAL_WORKER, EXPLORE_WORKER, PLAN_WORKER, VERIFY_WORKER |
| `AgentContext` | Session, depth counter, parent ref. `canNest()` checks maxDepth (3) |
| `AgentDefinitionFactory` | Creates role-specific definitions with scoped tools |
| `DefaultWorkerOrchestrator` | Depth-limited delegation, parallel via virtual threads |
| `CoordinatorRuntime` | Parallel worker execution |
| `CustomAgentDefinition` | Record: name, description, systemPrompt, model, allowedTools |
| `CustomAgentLoader` | Loads JSON agent defs from `.agents/` + `~/.enterprisewebagent/agents/` |

### teams (`runtime.teams`)

Multi-agent team collaboration.

| Class | Role |
|-------|------|
| `Team` | Record: id, name, members, created |
| `TeamMember` | Record: agentId, role, model, config |
| `TeamMessage` | Record: from, to, content, timestamp |
| `TeamRegistry` / `InMemoryTeamRegistry` | Team CRUD + message passing |

### skills (`runtime.skills`)

Loads markdown-based skills with frontmatter metadata from 3 sources.

| Class | Role |
|-------|------|
| `DefaultSkillLoader` | Loads from PROJECT → USER → MANAGED, deduplicates |
| `FrontmatterParser` | YAML frontmatter extraction |
| `SkillFileScanner` | `.md` file discovery |
| `SkillDefinition` | Record: name, content, origin, enabled |
| `SkillPromptExpander` | Expands skills into prompt sections |

### events (`runtime.events`)

Transport-agnostic event system with 10 event types.

```java
public sealed interface RuntimeEvent permits
    TurnStartedEvent, TokenDeltaEvent, ThinkingEvent,
    ToolRequestedEvent, ToolCompletedEvent,
    AskUserRequestedEvent,
    TaskStateChangedEvent, WorkerStateChangedEvent,
    TurnCompletedEvent, TurnFailedEvent;
```

| Class | Role |
|-------|------|
| `InMemoryEventPublisher` | Synchronous dispatch |
| `AsyncEventPublisher` | Virtual-thread async dispatch |
| `EventBuffer` | Buffered polling for clients |
| `EventSerializer` | JSON for WebSocket transport |

### memory (`runtime.memory`)

Project-level MEMORY.md + session-scoped memory.

| Class | Role |
|-------|------|
| `MemoryFileLoader` | Loads/saves MEMORY.md from project root (10KB limit) |
| `AutoMemorySummarizer` | Extracts decisions, errors, file changes from transcripts |
| `InMemorySessionMemory` | Session memory with project memory injection |
| `InMemoryMemoryStore` | Key-value store for memory entries |

### hooks (`runtime.hooks`)

Pre/post hooks for turns, tools, files, and sessions.

| Class | Role |
|-------|------|
| `Hook` | Record: id, type, command, order, enabled |
| `HookType` | Enum: PRE_TURN, POST_TURN, PRE_TOOL, POST_TOOL, ON_FILE_CHANGE, ON_SESSION_START, ON_SESSION_END |
| `HookRegistry` | Register/execute hooks in order |
| `HookExecutor` | Shell execution with 10s timeout, exit code 2 = abort |
| `HookConfigLoader` | Loads from `.agent/hooks.json` + global config |

### session (`runtime.session`)

Session lifecycle with tagging and export.

| Class | Role |
|-------|------|
| `Session` | Record: id, status, created, transcript |
| `SessionManager` | Interface: CRUD + tag + export |
| `JpaSessionManager` | Persistent (H2/Postgres) |
| `SessionExporter` | Export as markdown, JSON, or summary |
| `InMemorySessionTagStore` | Tag management |
| `SessionCompactionManager` | Transcript compaction |

### tasks (`runtime.tasks`)

Task lifecycle tracking with JPA persistence.

| Class | Role |
|-------|------|
| `TaskDefinition` | Record: id, sessionId, description, status |
| `TaskManager` / `JpaTaskManager` | Task CRUD with DB persistence |

### provider (`runtime.provider`)

5 LLM providers behind the Spring AI adapter boundary.

| Class | Role |
|-------|------|
| `ModelProvider` | Interface: prompt sections → response string |
| `DefaultModelProviderRegistry` | Named provider registry with default |
| `SpringAiModelProvider` | Delegates to Spring AI ChatModel |
| `StubModelProvider` | Fixed response for tests/dev |
| `ProviderModels` | 5-provider, 20-model catalog with prefix routing |
| `ModelRegistry` | Health detection, provider status tracking |
| `CopilotTokenProvider` | GitHub Copilot `gh auth token` |
| `CodexTokenProvider` | `~/.codex/auth.json` token management |

**Providers:** OpenAI, Anthropic, Ollama (local), GitHub Copilot, Codex
**Routing:** `copilot:gpt-4.1` → Copilot provider, `ollama:llama3.2` → Ollama provider

## App Layer

### REST API (`app.api`)

| Endpoint | Method | Controller |
|----------|--------|-----------|
| `POST /api/v1/sessions` | Create session | SessionController |
| `GET /api/v1/sessions/{id}` | Get session | SessionController |
| `POST /api/v1/sessions/{id}/turns` | Execute turn (with slash command intercept) | SessionController |
| `POST /api/v1/sessions/{id}/close` | Close session | SessionController |
| `POST /api/v1/sessions/{id}/tags` | Add tag | SessionController |
| `GET /api/v1/sessions/{id}/tags` | Get tags | SessionController |
| `GET /api/v1/sessions/{id}/export` | Export session | SessionController |
| `POST /api/v1/sessions/{id}/plan` | Create/get plan | SessionController |
| `PUT /api/v1/sessions/{id}/plan/steps/{stepId}` | Approve/reject plan step | SessionController |
| `POST /api/v1/sessions/{id}/plan/execute` | Execute approved plan | SessionController |
| `POST /api/v1/tasks` | Create task | TaskController |
| `GET /api/v1/tasks/{id}` | Get task | TaskController |
| `GET /api/v1/tasks` | List tasks | TaskController |
| `PUT /api/v1/tasks/{id}/status` | Update status | TaskController |
| `GET /api/v1/config` | Runtime info | ConfigController |
| `GET /api/v1/config/models` | Model catalog | ConfigController |
| `POST /api/v1/config/models/refresh` | Refresh model health | ConfigController |
| `GET /api/v1/sessions/{id}/cost` | Session cost | CostController |
| `GET /api/v1/cost/total` | Total cost | CostController |
| `GET /api/v1/permissions/mode` | Permission mode | PermissionController |
| `PUT /api/v1/permissions/mode` | Set permission mode | PermissionController |
| `GET /api/v1/permissions/rules` | List rules | PermissionController |
| `POST /api/v1/permissions/rules` | Add rule | PermissionController |
| `GET /api/v1/permissions/denials` | Recent denials | PermissionController |

### WebSocket (`app.ws`)

- **Endpoint:** `/ws/stream`
- Events: TurnStarted, TokenDelta, Thinking, ToolRequested, ToolCompleted, AskUserRequested, TaskStateChanged, WorkerStateChanged, TurnCompleted, TurnFailed

### Bean Wiring (`app.config`)

`RuntimeConfig` wires all runtime components including:
- Prompt assembly (cache, registry, assembler)
- Tool registry (6 registrars: BuiltIn, AgentTeam, Planning, Git, MemoryHook, MCP)
- Session/task managers (JPA-backed)
- Model provider registry (5 providers, prefix routing)
- Turn engine (with permission + cost integration)
- Command system (registry + dispatcher)
- Plan manager, hook registry, team registry

## CLI (`apps/cli`)

Picocli-based, no Spring dependency. Java 21 `HttpClient` + Gson. Shadow JAR + jlink packaging.

| Command | Purpose |
|---------|---------|
| `agent health` | Check server connectivity |
| `agent session` | Session CRUD + resume |
| `agent task` | Task management |
| `agent config` | Display configuration |

## Frontend (`apps/frontend`)

React 19 + TypeScript + Vite 6 + Bun. 100 modules, 283KB bundle.

| Feature | Location |
|---------|----------|
| Chat UI with streaming | `SessionPage.tsx` |
| Plan mode panel (approve/reject steps) | `PlanPanel.tsx` |
| Task panel | `TaskPanel.tsx` |
| Settings with model catalog | `SettingsPage.tsx` |
| Ask-user modal | `SessionPage.tsx` |
| Command autocomplete | `SessionPage.tsx` |
| Dark/light theme | `ThemeContext` |
| Error boundary | `ErrorBoundary.tsx` |
| Connection status indicator | WebSocket state |

## Data Flows

### Turn Execution Flow (with commands, permissions, cost)

```
Client input
  │
  ├─ Starts with "/"? → CommandDispatcher → CommandResult (may suppress turn)
  │
  ▼ (not a command)
SessionController → DefaultTurnEngine.executeTurn()
  │
  ├─ 1. Assemble prompt (precedence + plan context + memory + transcript)
  ├─ 2. Inject thinking instructions (if enabled/adaptive)
  ├─ 3. Call model provider (prefix-routed: copilot:gpt-4.1 → Copilot)
  ├─ 4. Parse <thinking> blocks → ThinkingEvent
  ├─ 5. Parse tool calls
  │     ├─ Permission check (BashSafetyAnalyzer, FilePathChecker)
  │     │   ├─ Allowed → execute tool
  │     │   ├─ Requires approval → AskUserRequestedEvent
  │     │   └─ Denied → DenialTracker + skip
  │     ├─ Plan mode filter (restrict tools per plan phase)
  │     └─ Append results → re-call model (max 10x)
  ├─ 6. Record cost (CostCalculator → SessionCostTracker)
  └─ 7. Execute hooks (POST_TURN)
```

### Team Collaboration Flow

```
Coordinator Turn
  │
  │  model calls team_create / agent / send_message tools
  ▼
TeamRegistry — manages teams, members, message queues
  │
AgentTool — spawns agent (custom def or ad-hoc)
  │
  ├─ Foreground: WorkerOrchestrator.delegate() → TurnResult
  └─ Background: TaskManager.create() → task ID (async execution)
```

## Implementation Phases — Complete Status

### Phase 1 — Core Parity (17 handoff steps) ✅
All runtime core behavior faithfully reproduced from the sacred text.

### Phase 2 — Production Readiness ✅
JPA persistence, 5 LLM providers, API key auth, structured logging/metrics, frontend polish, CLI packaging.

### Phase 3 — Custom Provider System ✅
ProviderModels catalog, ModelRegistry with health detection, Ollama/Copilot/Codex providers, prefix routing.

### Phase 4 — Functional Parity ✅
23 built-in tools, slash command system (15 commands), plan mode, extended thinking, permission system, cost tracking, MEMORY.md, git tools, hooks, team collaboration, custom agent definitions, session tagging/export.

## Metrics

| Metric | Value |
|--------|-------|
| Runtime source files | 187 |
| App layer source files | 28 |
| Test files | 104 |
| Total tests | 644 (0 failures) |
| Built-in tools | 23 + MCP bridge |
| Slash commands | 15 |
| LLM providers | 5 (OpenAI, Anthropic, Ollama, Copilot, Codex) |
| Event types | 10 |
| Frontend modules | 100 (283KB bundle) |

## What Remains — Future Phases

### Should-Do
1. **End-to-end integration tests** — full turn execution with real LLM
2. **WebSocket auth** — token-based WS authentication
3. **Workspace concept** — multi-project workspace management
4. **CLI interactive mode** — REPL-style chat in terminal
5. **Skill marketplace** — managed skill discovery and installation

### Nice-to-Have
6. **Remote execution** — SSH bridge, remote agent sessions
7. **Voice mode** — STT/TTS integration
8. **IDE integration** — VS Code extension
9. **Plugin SDK** — third-party plugin development kit
10. **Rate limiting + quotas** — per-client usage tracking
