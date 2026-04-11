package com.enterprisewebagent.runtime.planning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPlanManagerTest {

    private InMemoryPlanManager planManager;

    @BeforeEach
    void setUp() {
        planManager = new InMemoryPlanManager();
    }

    @Test
    void createPlan_storesAndReturnsPlan() {
        Plan plan = planManager.createPlan("s1", "Build a REST API");
        assertEquals("s1", plan.sessionId());
        assertEquals("Build a REST API", plan.goal());
        assertEquals(PlanMode.PLANNING, plan.mode());
        assertTrue(plan.steps().isEmpty());
        assertNotNull(plan.created());
    }

    @Test
    void getPlan_returnsEmptyForUnknownSession() {
        Optional<Plan> plan = planManager.getPlan("nonexistent");
        assertTrue(plan.isEmpty());
    }

    @Test
    void getPlan_returnsPlanAfterCreation() {
        planManager.createPlan("s1", "Build a REST API");
        Optional<Plan> plan = planManager.getPlan("s1");
        assertTrue(plan.isPresent());
        assertEquals("Build a REST API", plan.get().goal());
    }

    @Test
    void addStep_addsStepToPlan() {
        planManager.createPlan("s1", "Build API");
        PlanStep step = new PlanStep("step-1", "Create models");
        Plan updated = planManager.addStep("s1", step);

        assertEquals(1, updated.steps().size());
        assertEquals("step-1", updated.steps().get(0).id());
        assertEquals("Create models", updated.steps().get(0).description());
        assertEquals(PlanStepStatus.PENDING, updated.steps().get(0).status());
    }

    @Test
    void addStep_throwsForUnknownSession() {
        PlanStep step = new PlanStep("step-1", "Create models");
        assertThrows(IllegalStateException.class, () -> planManager.addStep("unknown", step));
    }

    @Test
    void addStep_preservesExistingSteps() {
        planManager.createPlan("s1", "Build API");
        planManager.addStep("s1", new PlanStep("step-1", "Create models"));
        Plan updated = planManager.addStep("s1", new PlanStep("step-2", "Add endpoints"));

        assertEquals(2, updated.steps().size());
        assertEquals("step-1", updated.steps().get(0).id());
        assertEquals("step-2", updated.steps().get(1).id());
    }

    @Test
    void updateStepStatus_updatesCorrectStep() {
        planManager.createPlan("s1", "Build API");
        planManager.addStep("s1", new PlanStep("step-1", "Create models"));
        planManager.addStep("s1", new PlanStep("step-2", "Add endpoints"));

        Plan updated = planManager.updateStepStatus("s1", "step-1", PlanStepStatus.APPROVED);
        assertEquals(PlanStepStatus.APPROVED, updated.steps().get(0).status());
        assertEquals(PlanStepStatus.PENDING, updated.steps().get(1).status());
    }

    @Test
    void updateStepStatus_throwsForUnknownStep() {
        planManager.createPlan("s1", "Build API");
        planManager.addStep("s1", new PlanStep("step-1", "Create models"));

        assertThrows(IllegalArgumentException.class,
                () -> planManager.updateStepStatus("s1", "nonexistent", PlanStepStatus.APPROVED));
    }

    @Test
    void updateStepStatus_throwsForUnknownSession() {
        assertThrows(IllegalStateException.class,
                () -> planManager.updateStepStatus("unknown", "step-1", PlanStepStatus.APPROVED));
    }

    @Test
    void setMode_changesMode() {
        planManager.createPlan("s1", "Build API");

        Plan updated = planManager.setMode("s1", PlanMode.REVIEWING);
        assertEquals(PlanMode.REVIEWING, updated.mode());

        updated = planManager.setMode("s1", PlanMode.EXECUTING);
        assertEquals(PlanMode.EXECUTING, updated.mode());

        updated = planManager.setMode("s1", PlanMode.OFF);
        assertEquals(PlanMode.OFF, updated.mode());
    }

    @Test
    void setMode_throwsForUnknownSession() {
        assertThrows(IllegalStateException.class,
                () -> planManager.setMode("unknown", PlanMode.REVIEWING));
    }

    @Test
    void removePlan_removesPlan() {
        planManager.createPlan("s1", "Build API");
        assertTrue(planManager.getPlan("s1").isPresent());

        planManager.removePlan("s1");
        assertTrue(planManager.getPlan("s1").isEmpty());
    }

    @Test
    void removePlan_noOpForUnknownSession() {
        planManager.removePlan("nonexistent"); // should not throw
    }

    @Test
    void modeTransitions_planningToReviewingToExecuting() {
        planManager.createPlan("s1", "Build API");
        assertEquals(PlanMode.PLANNING, planManager.getPlan("s1").get().mode());

        planManager.setMode("s1", PlanMode.REVIEWING);
        assertEquals(PlanMode.REVIEWING, planManager.getPlan("s1").get().mode());

        planManager.setMode("s1", PlanMode.EXECUTING);
        assertEquals(PlanMode.EXECUTING, planManager.getPlan("s1").get().mode());
    }
}
