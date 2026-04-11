package com.enterprisewebagent.runtime.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPlanManager implements PlanManager {

    private final ConcurrentHashMap<String, Plan> plans = new ConcurrentHashMap<>();

    @Override
    public Plan createPlan(String sessionId, String goal) {
        Plan plan = new Plan(sessionId, goal);
        plans.put(sessionId, plan);
        return plan;
    }

    @Override
    public Optional<Plan> getPlan(String sessionId) {
        return Optional.ofNullable(plans.get(sessionId));
    }

    @Override
    public Plan addStep(String sessionId, PlanStep step) {
        return plans.compute(sessionId, (key, existing) -> {
            if (existing == null) {
                throw new IllegalStateException("No plan exists for session: " + sessionId);
            }
            List<PlanStep> updated = new ArrayList<>(existing.steps());
            updated.add(step);
            return existing.withSteps(List.copyOf(updated));
        });
    }

    @Override
    public Plan updateStepStatus(String sessionId, String stepId, PlanStepStatus status) {
        return plans.compute(sessionId, (key, existing) -> {
            if (existing == null) {
                throw new IllegalStateException("No plan exists for session: " + sessionId);
            }
            List<PlanStep> updated = existing.steps().stream()
                    .map(step -> step.id().equals(stepId) ? step.withStatus(status) : step)
                    .toList();
            boolean found = existing.steps().stream().anyMatch(s -> s.id().equals(stepId));
            if (!found) {
                throw new IllegalArgumentException("Step not found: " + stepId);
            }
            return existing.withSteps(updated);
        });
    }

    @Override
    public Plan setMode(String sessionId, PlanMode mode) {
        return plans.compute(sessionId, (key, existing) -> {
            if (existing == null) {
                throw new IllegalStateException("No plan exists for session: " + sessionId);
            }
            return existing.withMode(mode);
        });
    }

    @Override
    public void removePlan(String sessionId) {
        plans.remove(sessionId);
    }
}
