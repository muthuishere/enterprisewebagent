package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.planning.PlanManager;
import com.enterprisewebagent.runtime.planning.Plan;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.Map;

public class EnterPlanModeTool implements ToolExecutor {

    private final PlanManager planManager;

    public EnterPlanModeTool(PlanManager planManager) {
        this.planManager = planManager;
    }

    @Override
    public String toolName() {
        return "enter_plan_mode";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();
        Object goalObj = args.get("goal");
        if (goalObj == null || goalObj.toString().isBlank()) {
            return new ToolResult(toolName(), "Missing required parameter: goal", false);
        }

        String goal = goalObj.toString();
        String sessionId = context.sessionId();

        if (planManager.getPlan(sessionId).isPresent()) {
            return new ToolResult(toolName(),
                    "Plan mode is already active for this session. Exit the current plan first.", false);
        }

        Plan plan = planManager.createPlan(sessionId, goal);
        return new ToolResult(toolName(),
                "Plan mode activated. Goal: " + plan.goal() +
                ". You are now in PLANNING mode. Create a step-by-step plan before executing.", true);
    }
}
