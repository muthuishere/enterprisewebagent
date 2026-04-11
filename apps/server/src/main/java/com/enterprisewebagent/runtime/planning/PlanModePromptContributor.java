package com.enterprisewebagent.runtime.planning;

import com.enterprisewebagent.runtime.prompt.PromptSection;

import java.util.Optional;

public class PlanModePromptContributor {

    private final PlanManager planManager;

    public PlanModePromptContributor(PlanManager planManager) {
        this.planManager = planManager;
    }

    public Optional<PromptSection> contribute(String sessionId) {
        return planManager.getPlan(sessionId).map(plan -> {
            String content = buildPlanPrompt(plan);
            return new PromptSection("plan_mode", content, false);
        });
    }

    private String buildPlanPrompt(Plan plan) {
        StringBuilder sb = new StringBuilder();

        switch (plan.mode()) {
            case PLANNING -> {
                sb.append("You are in PLANNING mode. Create a step-by-step plan to accomplish the goal.\n");
                sb.append("Do not execute any actions yet. Only outline the steps needed.\n");
                sb.append("Use only read-only tools (file_read, grep, glob) to gather context.\n\n");
                sb.append("Goal: ").append(plan.goal()).append("\n");
                appendExistingSteps(sb, plan);
            }
            case REVIEWING -> {
                sb.append("You are in REVIEWING mode. The user is reviewing the plan steps.\n");
                sb.append("Wait for the user to approve or reject each step.\n\n");
                sb.append("Goal: ").append(plan.goal()).append("\n");
                appendExistingSteps(sb, plan);
            }
            case EXECUTING -> {
                sb.append("You are in EXECUTING mode. Execute the following approved plan steps in order.\n");
                sb.append("Skip rejected steps. Report progress on each step.\n\n");
                sb.append("Goal: ").append(plan.goal()).append("\n");
                appendExistingSteps(sb, plan);
            }
            case OFF -> {
                // No plan prompt when off
            }
        }

        return sb.toString();
    }

    private void appendExistingSteps(StringBuilder sb, Plan plan) {
        if (plan.steps().isEmpty()) {
            return;
        }
        sb.append("\nPlan steps:\n");
        for (int i = 0; i < plan.steps().size(); i++) {
            PlanStep step = plan.steps().get(i);
            sb.append(String.format("  %d. [%s] %s", i + 1, step.status().name(), step.description()));
            if (step.result() != null) {
                sb.append(" → ").append(step.result());
            }
            sb.append("\n");
        }
    }
}
