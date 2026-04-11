package com.enterprisewebagent.runtime.tools;

public interface ToolExecutor {
    ToolResult execute(ToolInvocation invocation, ToolContext context);
    String toolName();
}
