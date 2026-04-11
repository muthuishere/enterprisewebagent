package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.planning.*;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExitPlanModeToolTest {

    private InMemoryPlanManager planManager;
    private ExitPlanModeTool tool;

    @BeforeEach
    void setUp() {
        planManager = new InMemoryPlanManager();
        tool = new ExitPlanModeTool(planManager);
    }

    @Test
    void toolName() {
        assertEquals("exit_plan_mode", tool.toolName());
    }

    @Test
    void execute_removesPlan() {
        planManager.createPlan("s1", "Build API");
        planManager.addStep("s1", new PlanStep("step-1", "Create models"));

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("exit_plan_mode", Map.of());

        ToolResult result = tool.execute(invocation, ctx);

        assertTrue(result.success());
        assertTrue(result.output().contains("Plan mode deactivated"));
        assertTrue(result.output().contains("Build API"));
        assertTrue(planManager.getPlan("s1").isEmpty());
    }

    @Test
    void execute_includesSummaryWithStepCounts() {
        planManager.createPlan("s1", "Build API");
        planManager.addStep("s1", new PlanStep("step-1", "Create models", PlanStepStatus.COMPLETED, "Done"));
        planManager.addStep("s1", new PlanStep("step-2", "Add endpoints", PlanStepStatus.FAILED, "Error"));
        planManager.addStep("s1", new PlanStep("step-3", "Write tests"));

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("exit_plan_mode", Map.of());

        ToolResult result = tool.execute(invocation, ctx);

        assertTrue(result.success());
        assertTrue(result.output().contains("Completed: 1"));
        assertTrue(result.output().contains("Failed: 1"));
        assertTrue(result.output().contains("Pending: 1"));
    }

    @Test
    void execute_failsWhenNoPlan() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("exit_plan_mode", Map.of());

        ToolResult result = tool.execute(invocation, ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("No active plan"));
    }
}
