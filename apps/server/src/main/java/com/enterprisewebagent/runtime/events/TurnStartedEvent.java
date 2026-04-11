package com.enterprisewebagent.runtime.events;

import java.time.Instant;

public record TurnStartedEvent(String sessionId, Instant timestamp) implements RuntimeEvent {
}
