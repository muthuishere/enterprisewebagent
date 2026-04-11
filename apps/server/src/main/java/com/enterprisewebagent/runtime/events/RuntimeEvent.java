package com.enterprisewebagent.runtime.events;

public sealed interface RuntimeEvent permits
        TurnStartedEvent,
        TokenDeltaEvent,
        ToolRequestedEvent,
        ToolCompletedEvent,
        AskUserRequestedEvent,
        TaskStateChangedEvent,
        WorkerStateChangedEvent,
        TurnCompletedEvent,
        TurnFailedEvent,
        ThinkingEvent {
}
