package com.enterprisewebagent.runtime.planning;

public record PlanStep(String id, String description, PlanStepStatus status, String result) {

    public PlanStep(String id, String description) {
        this(id, description, PlanStepStatus.PENDING, null);
    }

    public PlanStep withStatus(PlanStepStatus newStatus) {
        return new PlanStep(id, description, newStatus, result);
    }

    public PlanStep withResult(String newResult) {
        return new PlanStep(id, description, status, newResult);
    }

    public PlanStep withStatusAndResult(PlanStepStatus newStatus, String newResult) {
        return new PlanStep(id, description, newStatus, newResult);
    }
}
