package com.enterprisewebagent.runtime.permissions;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolFilter;

import java.util.List;

public class PermissionToolFilter implements ToolFilter {

    private final PermissionEvaluator evaluator;

    public PermissionToolFilter(PermissionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public List<ToolDefinition> filter(List<ToolDefinition> tools, ToolContext context) {
        if (evaluator.getMode() == PermissionMode.LOCKED) {
            return List.of();
        }
        // Tool-level filtering: only remove tools with explicit TOOL-scope DENY rules.
        // Argument-dependent checks (bash safety, file paths) run at execution time.
        return tools.stream()
            .filter(tool -> !isExplicitlyDenied(tool.name()))
            .toList();
    }

    private boolean isExplicitlyDenied(String toolName) {
        for (PermissionRule rule : evaluator.getRules()) {
            if (rule.scope() == PermissionScope.TOOL && rule.type() == PermissionType.DENY) {
                try {
                    if (toolName.matches(rule.pattern()) || toolName.equals(rule.pattern())) {
                        return true;
                    }
                } catch (Exception e) {
                    if (toolName.equals(rule.pattern())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public PermissionEvaluator getEvaluator() {
        return evaluator;
    }
}
