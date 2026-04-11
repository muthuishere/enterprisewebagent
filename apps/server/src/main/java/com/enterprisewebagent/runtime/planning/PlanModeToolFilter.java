package com.enterprisewebagent.runtime.planning;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolFilter;

import java.util.List;
import java.util.Set;

public class PlanModeToolFilter implements ToolFilter {

    private static final Set<String> PLANNING_TOOLS = Set.of(
            "file_read", "grep", "glob", "ask_user"
    );

    private static final Set<String> REVIEWING_TOOLS = Set.of(
            "ask_user"
    );

    private final PlanManager planManager;

    public PlanModeToolFilter(PlanManager planManager) {
        this.planManager = planManager;
    }

    @Override
    public List<ToolDefinition> filter(List<ToolDefinition> tools, ToolContext context) {
        PlanMode mode = planManager.getPlan(context.sessionId())
                .map(Plan::mode)
                .orElse(PlanMode.OFF);

        return switch (mode) {
            case OFF, EXECUTING -> tools;
            case PLANNING -> tools.stream()
                    .filter(tool -> PLANNING_TOOLS.contains(tool.name()))
                    .toList();
            case REVIEWING -> tools.stream()
                    .filter(tool -> REVIEWING_TOOLS.contains(tool.name()))
                    .toList();
        };
    }
}
