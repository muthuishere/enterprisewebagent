package com.enterprisewebagent.runtime.events;

import java.time.Instant;

public record TurnFailedEvent(String sessionId, String error, Instant timestamp) implements RuntimeEvent {
}
