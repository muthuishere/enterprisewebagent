package com.enterprisewebagent.runtime.events;

import java.util.Map;

public record ToolRequestedEvent(String sessionId, String toolName, Map<String, Object> arguments) implements RuntimeEvent {
}
