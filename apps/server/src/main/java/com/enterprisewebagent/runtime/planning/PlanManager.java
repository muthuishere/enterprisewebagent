package com.enterprisewebagent.runtime.planning;

import java.util.Optional;

public interface PlanManager {
    Plan createPlan(String sessionId, String goal);
    Optional<Plan> getPlan(String sessionId);
    Plan addStep(String sessionId, PlanStep step);
    Plan updateStepStatus(String sessionId, String stepId, PlanStepStatus status);
    Plan setMode(String sessionId, PlanMode mode);
    void removePlan(String sessionId);
}
