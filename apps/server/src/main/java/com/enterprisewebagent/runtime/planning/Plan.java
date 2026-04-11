package com.enterprisewebagent.runtime.planning;

import java.time.Instant;
import java.util.List;

public record Plan(String sessionId, String goal, List<PlanStep> steps, PlanMode mode, Instant created) {

    public Plan(String sessionId, String goal) {
        this(sessionId, goal, List.of(), PlanMode.PLANNING, Instant.now());
    }

    public Plan withSteps(List<PlanStep> newSteps) {
        return new Plan(sessionId, goal, newSteps, mode, created);
    }

    public Plan withMode(PlanMode newMode) {
        return new Plan(sessionId, goal, steps, newMode, created);
    }
}
