package com.enterprisewebagent.runtime.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultToolRegistry implements ToolRegistry {

    private final ConcurrentHashMap<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ToolExecutor> executors = new ConcurrentHashMap<>();
    private final DenyRuleFilter denyRuleFilter = new DenyRuleFilter();
    private final ModeToolFilter modeToolFilter = new ModeToolFilter();

    @Override
    public void register(ToolDefinition tool) {
        tools.put(tool.name(), tool);
    }

    public void registerExecutor(ToolExecutor executor) {
        String name = executor.toolName();
        executors.put(name, executor);

        if (!tools.containsKey(name)) {
            ToolDefinition definition = new ToolDefinition(
                    name,
                    name,
                    Map.of(),
                    true
            );
            tools.put(name, definition);
        }
    }

    @Override
    public void deny(String toolName) {
        denyRuleFilter.addDenyRule(toolName);
    }

    @Override
    public List<ToolDefinition> resolveTools(ToolContext context) {
        List<ToolDefinition> allTools = new ArrayList<>(tools.values());

        List<ToolDefinition> afterDeny = denyRuleFilter.filter(allTools, context);
        List<ToolDefinition> afterMode = modeToolFilter.filter(afterDeny, context);

        List<ToolDefinition> sorted = new ArrayList<>(afterMode);
        sorted.sort(Comparator.comparing(ToolDefinition::name));

        return Collections.unmodifiableList(sorted);
    }

    public Optional<ToolExecutor> getExecutor(String toolName) {
        return Optional.ofNullable(executors.get(toolName));
    }

    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        ToolExecutor executor = executors.get(invocation.name());
        if (executor == null) {
            return new ToolResult(invocation.name(), "Unknown tool: " + invocation.name(), false);
        }
        return executor.execute(invocation, context);
    }

    DenyRuleFilter getDenyRuleFilter() {
        return denyRuleFilter;
    }
}
