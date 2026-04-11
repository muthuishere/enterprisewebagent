package com.enterprisewebagent.runtime.tools;

import java.util.List;
import java.util.Set;

public class ModeToolFilter implements ToolFilter {

    private static final Set<String> SIMPLE_MODE_TOOLS = Set.of(
            "file_read", "file_edit", "shell", "ask_user"
    );

    @Override
    public List<ToolDefinition> filter(List<ToolDefinition> tools, ToolContext context) {
        ToolMode mode = resolveMode(context.mode());

        return switch (mode) {
            case NORMAL, COORDINATOR -> tools;
            case SIMPLE -> tools.stream()
                    .filter(tool -> SIMPLE_MODE_TOOLS.contains(tool.name()))
                    .toList();
            case WORKER -> {
                Set<String> capabilities = context.capabilities();
                if (capabilities == null || capabilities.isEmpty()) {
                    yield List.of();
                }
                yield tools.stream()
                        .filter(tool -> capabilities.contains(tool.name()))
                        .toList();
            }
        };
    }

    private ToolMode resolveMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return ToolMode.NORMAL;
        }
        try {
            return ToolMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToolMode.NORMAL;
        }
    }
}
