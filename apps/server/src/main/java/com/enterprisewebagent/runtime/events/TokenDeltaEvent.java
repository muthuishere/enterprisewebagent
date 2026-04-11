package com.enterprisewebagent.runtime.events;

public record TokenDeltaEvent(String sessionId, String text) implements RuntimeEvent {
}
