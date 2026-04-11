package com.enterprisewebagent.runtime.agents;

import java.util.Set;

public record AgentDefinition(String id, AgentRole role, String promptSurface, Set<String> allowedTools) {
}
