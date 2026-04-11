package com.enterprisewebagent.runtime.agents;

import java.util.Map;

public record AgentContext(
    String sessionId,
    AgentRole role,
    Map<String, String> inheritedContext,
    String parentWorkerId,
    int depth,
    int maxDepth
) {
    public AgentContext(String sessionId, AgentRole role, Map<String, String> inheritedContext) {
        this(sessionId, role, inheritedContext, null, 0, 3);
    }

    public AgentContext childContext(String workerId, AgentRole childRole) {
        return new AgentContext(sessionId, childRole, inheritedContext, workerId, depth + 1, maxDepth);
    }

    public boolean canNest() {
        return depth < maxDepth;
    }
}
