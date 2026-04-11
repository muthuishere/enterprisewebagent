package com.enterprisewebagent.runtime.planning;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlanModeToolFilterTest {

    private InMemoryPlanManager planManager;
    private PlanModeToolFilter filter;

    private final List<ToolDefinition> allTools = List.of(
            new ToolDefinition("file_read", "Read files", Map.of(), true),
            new ToolDefinition("file_edit", "Edit files", Map.of(), true),
            new ToolDefinition("shell", "Run shell commands", Map.of(), true),
            new ToolDefinition("grep", "Search files", Map.of(), true),
            new ToolDefinition("glob", "Pattern match files", Map.of(), true),
            new ToolDefinition("ask_user", "Ask user a question", Map.of(), true),
            new ToolDefinition("web_fetch", "Fetch web page", Map.of(), true)
    );

    @BeforeEach
    void setUp() {
        planManager = new InMemoryPlanManager();
        filter = new PlanModeToolFilter(planManager);
    }

    @Test
    void offMode_returnsAllTools() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> result = filter.filter(allTools, ctx);
        assertEquals(allTools.size(), result.size());
    }

    @Test
    void noPlan_returnsAllTools() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> result = filter.filter(allTools, ctx);
        assertEquals(allTools.size(), result.size());
    }

    @Test
    void planningMode_onlyAllowsReadOnlyTools() {
        planManager.createPlan("s1", "Build API");
        // mode is PLANNING by default after creation

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> result = filter.filter(allTools, ctx);

        List<String> names = result.stream().map(ToolDefinition::name).toList();
        assertEquals(4, names.size());
        assertTrue(names.contains("file_read"));
        assertTrue(names.contains("grep"));
        assertTrue(names.contains("glob"));
        assertTrue(names.contains("ask_user"));
        assertFalse(names.contains("file_edit"));
        assertFalse(names.contains("shell"));
        assertFalse(names.contains("web_fetch"));
    }

    @Test
    void reviewingMode_onlyAllowsAskUser() {
        planManager.createPlan("s1", "Build API");
        planManager.setMode("s1", PlanMode.REVIEWING);

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> result = filter.filter(allTools, ctx);

        List<String> names = result.stream().map(ToolDefinition::name).toList();
        assertEquals(1, names.size());
        assertTrue(names.contains("ask_user"));
    }

    @Test
    void executingMode_returnsAllTools() {
        planManager.createPlan("s1", "Build API");
        planManager.setMode("s1", PlanMode.EXECUTING);

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> result = filter.filter(allTools, ctx);
        assertEquals(allTools.size(), result.size());
    }

    @Test
    void differentSessions_filteredIndependently() {
        planManager.createPlan("s1", "Build API");
        // s1 is in PLANNING mode

        // s2 has no plan
        ToolContext ctx1 = new ToolContext("s1", "NORMAL", Set.of());
        ToolContext ctx2 = new ToolContext("s2", "NORMAL", Set.of());

        List<ToolDefinition> result1 = filter.filter(allTools, ctx1);
        List<ToolDefinition> result2 = filter.filter(allTools, ctx2);

        assertEquals(4, result1.size()); // planning tools only
        assertEquals(allTools.size(), result2.size()); // all tools
    }
}
