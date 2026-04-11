package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.Map;
import java.util.Set;

public class WorkerDelegationTool implements ToolExecutor {

    private static final Set<String> VALID_ROLES = Set.of("general", "explore", "plan", "verify");

    @Override
    public String toolName() {
        return "worker_delegate";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object roleObj = args.get("worker_role");
        Object descObj = args.get("task_description");

        if (roleObj == null || descObj == null) {
            return new ToolResult(toolName(), "Missing required parameters: worker_role, task_description", false);
        }

        String role = roleObj.toString();
        String description = descObj.toString();

        if (!VALID_ROLES.contains(role)) {
            return new ToolResult(toolName(),
                    "Invalid worker_role: " + role + ". Must be one of: " + VALID_ROLES, false);
        }

        return new ToolResult(toolName(),
                "Delegation queued — role: " + role + ", task: " + description, true);
    }
}
