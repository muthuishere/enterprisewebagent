package com.enterprisewebagent.runtime.planning;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PlanModePromptContributorTest {

    private InMemoryPlanManager planManager;
    private PlanModePromptContributor contributor;

    @BeforeEach
    void setUp() {
        planManager = new InMemoryPlanManager();
        contributor = new PlanModePromptContributor(planManager);
    }

    @Test
    void contribute_returnsEmptyWhenNoPlan() {
        Optional<PromptSection> section = contributor.contribute("s1");
        assertTrue(section.isEmpty());
    }

    @Test
    void contribute_inPlanningMode_containsPlanningInstructions() {
        planManager.createPlan("s1", "Build API");

        Optional<PromptSection> section = contributor.contribute("s1");
        assertTrue(section.isPresent());
        assertEquals("plan_mode", section.get().name());
        assertTrue(section.get().content().contains("PLANNING mode"));
        assertTrue(section.get().content().contains("step-by-step plan"));
        assertTrue(section.get().content().contains("Build API"));
        assertFalse(section.get().cached());
    }

    @Test
    void contribute_inReviewingMode_containsReviewingInstructions() {
        planManager.createPlan("s1", "Build API");
        planManager.setMode("s1", PlanMode.REVIEWING);

        Optional<PromptSection> section = contributor.contribute("s1");
        assertTrue(section.isPresent());
        assertTrue(section.get().content().contains("REVIEWING mode"));
        assertTrue(section.get().content().contains("approve or reject"));
    }

    @Test
    void contribute_inExecutingMode_containsExecutionInstructions() {
        planManager.createPlan("s1", "Build API");
        planManager.setMode("s1", PlanMode.EXECUTING);

        Optional<PromptSection> section = contributor.contribute("s1");
        assertTrue(section.isPresent());
        assertTrue(section.get().content().contains("EXECUTING mode"));
        assertTrue(section.get().content().contains("approved plan steps"));
    }

    @Test
    void contribute_includesStepDetails() {
        planManager.createPlan("s1", "Build API");
        planManager.addStep("s1", new PlanStep("step-1", "Create data models"));
        planManager.addStep("s1", new PlanStep("step-2", "Add REST endpoints"));

        Optional<PromptSection> section = contributor.contribute("s1");
        assertTrue(section.isPresent());
        String content = section.get().content();
        assertTrue(content.contains("Create data models"));
        assertTrue(content.contains("Add REST endpoints"));
        assertTrue(content.contains("[PENDING]"));
    }

    @Test
    void contribute_includesStepResults() {
        planManager.createPlan("s1", "Build API");
        PlanStep step = new PlanStep("step-1", "Create models", PlanStepStatus.COMPLETED, "Models created");
        planManager.addStep("s1", step);

        Optional<PromptSection> section = contributor.contribute("s1");
        assertTrue(section.isPresent());
        assertTrue(section.get().content().contains("Models created"));
        assertTrue(section.get().content().contains("[COMPLETED]"));
    }

    @Test
    void contribute_offMode_noContent() {
        planManager.createPlan("s1", "Build API");
        planManager.setMode("s1", PlanMode.OFF);

        Optional<PromptSection> section = contributor.contribute("s1");
        assertTrue(section.isPresent());
        // OFF mode produces an empty string
        assertTrue(section.get().content().isEmpty());
    }
}
