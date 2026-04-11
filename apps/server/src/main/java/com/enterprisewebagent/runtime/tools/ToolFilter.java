package com.enterprisewebagent.runtime.tools;

import java.util.List;

public interface ToolFilter {
    List<ToolDefinition> filter(List<ToolDefinition> tools, ToolContext context);
}
