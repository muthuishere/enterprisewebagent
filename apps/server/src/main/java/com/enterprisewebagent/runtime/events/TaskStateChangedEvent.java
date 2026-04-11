package com.enterprisewebagent.runtime.events;

public record TaskStateChangedEvent(String sessionId, String taskId, String newState) implements RuntimeEvent {
}
