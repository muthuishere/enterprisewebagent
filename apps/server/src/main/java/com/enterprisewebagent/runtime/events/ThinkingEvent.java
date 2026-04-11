package com.enterprisewebagent.runtime.events;

import java.time.Instant;

public record ThinkingEvent(String sessionId, String thinkingContent, Instant timestamp) implements RuntimeEvent {
}
