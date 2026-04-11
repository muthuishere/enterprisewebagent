package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.planning.Plan;
import com.enterprisewebagent.runtime.planning.PlanManager;
import com.enterprisewebagent.runtime.planning.PlanStep;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.Optional;

public class ExitPlanModeTool implements ToolExecutor {

    private final PlanManager planManager;

    public ExitPlanModeTool(PlanManager planManager) {
        this.planManager = planManager;
    }

    @Override
    public String toolName() {
        return "exit_plan_mode";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        String sessionId = context.sessionId();
        Optional<Plan> planOpt = planManager.getPlan(sessionId);

        if (planOpt.isEmpty()) {
            return new ToolResult(toolName(), "No active plan for this session.", false);
        }

        Plan plan = planOpt.get();
        String summary = buildSummary(plan);
        planManager.removePlan(sessionId);

        return new ToolResult(toolName(), "Plan mode deactivated.\n" + summary, true);
    }

    private String buildSummary(Plan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Plan summary for goal: ").append(plan.goal()).append("\n");
        sb.append("Total steps: ").append(plan.steps().size()).append("\n");

        long completed = plan.steps().stream()
                .filter(s -> s.status() == com.enterprisewebagent.runtime.planning.PlanStepStatus.COMPLETED)
                .count();
        long failed = plan.steps().stream()
                .filter(s -> s.status() == com.enterprisewebagent.runtime.planning.PlanStepStatus.FAILED)
                .count();
        long pending = plan.steps().stream()
                .filter(s -> s.status() == com.enterprisewebagent.runtime.planning.PlanStepStatus.PENDING)
                .count();

        sb.append("Completed: ").append(completed).append("\n");
        sb.append("Failed: ").append(failed).append("\n");
        sb.append("Pending: ").append(pending).append("\n");

        for (PlanStep step : plan.steps()) {
            sb.append("  - [").append(step.status().name()).append("] ").append(step.description());
            if (step.result() != null) {
                sb.append(" → ").append(step.result());
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
