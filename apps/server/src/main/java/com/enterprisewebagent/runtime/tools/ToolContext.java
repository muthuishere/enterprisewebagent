package com.enterprisewebagent.runtime.tools;

import java.util.Set;

public record ToolContext(String sessionId, String mode, Set<String> capabilities) {
}
