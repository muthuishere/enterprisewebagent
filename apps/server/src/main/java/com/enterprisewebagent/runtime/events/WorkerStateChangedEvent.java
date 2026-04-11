package com.enterprisewebagent.runtime.events;

public record WorkerStateChangedEvent(String sessionId, String workerId, String newState) implements RuntimeEvent {
}
