package com.enterprisewebagent.runtime.events;

import com.enterprisewebagent.runtime.tools.ToolResult;

public record ToolCompletedEvent(String sessionId, String toolName, ToolResult result) implements RuntimeEvent {
}
