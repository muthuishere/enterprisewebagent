package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.Map;

public class TaskStopTool implements ToolExecutor {

    @Override
    public String toolName() {
        return "task_stop";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object taskIdObj = args.get("task_id");
        if (taskIdObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: task_id", false);
        }

        String taskId = taskIdObj.toString();
        String reason = args.containsKey("reason") ? args.get("reason").toString() : "no reason provided";

        return new ToolResult(toolName(),
                "Stop requested for task " + taskId + " (reason: " + reason + ")", true);
    }
}
