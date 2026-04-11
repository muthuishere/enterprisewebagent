package com.enterprisewebagent.runtime.tools;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DenyRuleFilter implements ToolFilter {

    private final Set<String> deniedTools = ConcurrentHashMap.newKeySet();

    @Override
    public List<ToolDefinition> filter(List<ToolDefinition> tools, ToolContext context) {
        return tools.stream()
                .filter(tool -> !deniedTools.contains(tool.name()))
                .toList();
    }

    public void addDenyRule(String toolName) {
        deniedTools.add(toolName);
    }

    public void removeDenyRule(String toolName) {
        deniedTools.remove(toolName);
    }

    public Set<String> getDeniedTools() {
        return Collections.unmodifiableSet(deniedTools);
    }
}
