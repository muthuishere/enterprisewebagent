package com.enterprisewebagent.runtime.tools;

import java.util.List;

public interface ToolRegistry {
    List<ToolDefinition> resolveTools(ToolContext context);
    void register(ToolDefinition tool);
    void deny(String toolName);
}
