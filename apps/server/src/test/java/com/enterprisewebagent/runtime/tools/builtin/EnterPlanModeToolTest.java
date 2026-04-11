package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.planning.InMemoryPlanManager;
import com.enterprisewebagent.runtime.planning.PlanManager;
import com.enterprisewebagent.runtime.planning.PlanMode;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EnterPlanModeToolTest {

    private PlanManager planManager;
    private EnterPlanModeTool tool;

    @BeforeEach
    void setUp() {
        planManager = new InMemoryPlanManager();
        tool = new EnterPlanModeTool(planManager);
    }

    @Test
    void toolName() {
        assertEquals("enter_plan_mode", tool.toolName());
    }

    @Test
    void execute_createsPlan() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("enter_plan_mode", Map.of("goal", "Build a REST API"));

        ToolResult result = tool.execute(invocation, ctx);

        assertTrue(result.success());
        assertTrue(result.output().contains("Plan mode activated"));
        assertTrue(result.output().contains("Build a REST API"));
        assertTrue(planManager.getPlan("s1").isPresent());
        assertEquals(PlanMode.PLANNING, planManager.getPlan("s1").get().mode());
    }

    @Test
    void execute_failsWhenPlanAlreadyExists() {
        planManager.createPlan("s1", "Existing plan");
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("enter_plan_mode", Map.of("goal", "New plan"));

        ToolResult result = tool.execute(invocation, ctx);

        assertFalse(result.success());
        assertTrue(result.output().contains("already active"));
    }

    @Test
    void execute_failsWithMissingGoal() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("enter_plan_mode", Map.of());

        ToolResult result = tool.execute(invocation, ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }

    @Test
    void execute_failsWithBlankGoal() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("enter_plan_mode", Map.of("goal", "   "));

        ToolResult result = tool.execute(invocation, ctx);
        assertFalse(result.success());
    }
}
