# Parity Verification — Step 17

Verified against: `IMPLEMENTATION_HANDOFF.md`, `lld-core-parity.md`, `reference-current-core-prompt-system.md`

## Summary

| # | Area | Status |
|---|------|--------|
| 1 | Prompt Precedence | **PASS** |
| 2 | Prompt Section Caching | **PASS** |
| 3 | Prompt Catalog (12 surfaces) | **PASS** |
| 4 | Tool Ordering | **PASS** |
| 5 | Deny-Rule Filtering | **PASS** |
| 6 | Worker Role Prompts | **PASS** |
| 7 | Coordinator Behavior | **PASS** |
| 8 | Session Persistence | **GAP** |
| 9 | Streaming Events | **PASS** |
| 10 | Ask-User Escalation | **PARTIAL** |
| 11 | Turn Engine | **PASS** |
| 12 | Compaction/Recovery | **PASS** |
| 13 | Skill Loading | **PASS** |
| 14 | Transport (REST + WS) | **PASS** |

**Result: 12 PASS, 1 GAP, 1 PARTIAL out of 14 parity areas.**

---

## Detail

### 1. Prompt Precedence (5-level) — PASS ✅

`PromptPrecedence` enum defines: OVERRIDE > COORDINATOR > CUSTOM_AGENT > USER_SYSTEM > DEFAULT.
`DefaultPromptAssembler.assemble()` switches on `context.activePrecedence()`. OVERRIDE returns early (skips append/memory). All other levels fall through to append + memory.

### 2. Prompt Section Stability — PASS ✅

`PromptCatalog` marks prompts 001–005 as `cached=true` (static prefix), 006 and 012 as `cached=false` (dynamic). Cached sections resolve through `InMemoryPromptSectionCache`; uncached always resolve fresh. `clearDynamic()` only removes uncached keys.

### 3. Prompt Catalog Mapping — PASS ✅

12 entries: `SYSTEM_IDENTITY` → prompt_001 through `ASK_USER` → prompt_012. All map 1:1 to `docs/prompts/`.

### 4. Tool Ordering — PASS ✅

`DefaultToolRegistry.resolveTools()` sorts by `Comparator.comparing(ToolDefinition::name)` — alphabetical, deterministic.

### 5. Deny-Rule Filtering — PASS ✅

`DenyRuleFilter` applied before mode filtering in the pipeline: deny → mode → sort.

### 6. Worker Role Prompts — PASS ✅

`WorkerPromptResolver` maps each `AgentRole` to its `PromptCatalog` entry. `AgentDefinitionFactory` creates role-specific scoped tool sets.

### 7. Coordinator Behavior — PASS ✅

`CoordinatorRuntime` delegates via `WorkerOrchestrator.delegate()`. Depth limiting via `AgentContext.canNest()` (maxDepth=3). Supports sequential and parallel execution.

### 8. Session Persistence — GAP ⚠️

`InMemorySessionManager` uses `ConcurrentHashMap` — purely in-memory. Step 10 of the handoff requires durable state. **No DB or file-backed persistence yet.** Session resume works within a running process but not across restarts.

### 9. Streaming Events — PASS ✅

8 event types via sealed `RuntimeEvent` interface. `StreamingWebSocketHandler` bridges to WebSocket clients. `EventSerializer` handles JSON transport.

### 10. Ask-User Escalation — PARTIAL ⚠️

`AskUserTool` returns `PENDING_USER_INPUT: <question>` in the tool result, which re-enters the turn engine. However, no dedicated `AskUserEvent` is published to WebSocket — clients must parse model text to detect escalation. **Gap: missing a first-class event for ask-user.**

### 11. Turn Engine — PASS ✅

Full loop: prompt → model → parse → execute tools → re-prompt. Max 10 iterations. Events published at each stage. Token estimation tracked.

### 12. Compaction/Recovery — PASS ✅

3 strategies (CLEAR, COMPACT, TRUNCATE). `SessionCompactionManager` orchestrates compaction with cache clearing. `PromptBudgetCalculator` provides budget-aware auto-detection.

### 13. Skill Loading — PASS ✅

PROJECT → USER → MANAGED load order. Frontmatter parsing, deduplication (first-seen wins), enable/disable filtering, trust origin tracking.

### 14. Transport — PASS ✅

Session CRUD + turn execution, Task CRUD, Config endpoint, WebSocket streaming. All wired through `RuntimeConfig`.

---

## Known Gaps for Phase 2

1. **Session persistence** — add file-backed or DB storage behind `SessionManager` interface
2. **Ask-user event** — publish a dedicated `AskUserRequested` event so WebSocket clients get a structured signal
