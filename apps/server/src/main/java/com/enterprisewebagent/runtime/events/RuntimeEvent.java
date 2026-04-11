package com.enterprisewebagent.runtime.events;

public sealed interface RuntimeEvent permits
        TurnStartedEvent,
        TokenDeltaEvent,
        ToolRequestedEvent,
        ToolCompletedEvent,
        TaskStateChangedEvent,
        WorkerStateChangedEvent,
        TurnCompletedEvent,
        TurnFailedEvent {
}
