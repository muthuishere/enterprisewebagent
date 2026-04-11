package com.enterprisewebagent.runtime.events;

import java.time.Instant;

public record TurnCompletedEvent(String sessionId, String output, Instant timestamp) implements RuntimeEvent {
}
