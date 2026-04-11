package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.agents.AgentContext;
import com.enterprisewebagent.runtime.agents.AgentDefinition;
import com.enterprisewebagent.runtime.agents.WorkerOrchestrator;
import com.enterprisewebagent.runtime.agents.WorkerResult;
import com.enterprisewebagent.runtime.agents.custom.CustomAgentDefinition;
import com.enterprisewebagent.runtime.agents.custom.CustomAgentLoader;
import com.enterprisewebagent.runtime.tasks.InMemoryTaskManager;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolTest {

    private AgentTool tool;
    private TaskManager taskManager;
    private CustomAgentLoader agentLoader;
    private StubWorkerOrchestrator orchestrator;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        orchestrator = new StubWorkerOrchestrator();
        taskManager = new InMemoryTaskManager();
        agentLoader = new CustomAgentLoader(List.of(Path.of("/non/existent")));
        tool = new AgentTool(orchestrator, taskManager, agentLoader);
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void missingNameReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("agent", Map.of("prompt", "do something"));
        ToolResult result = tool.execute(inv, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: name"));
    }

    @Test
    void missingPromptReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("agent", Map.of("name", "test-agent"));
        ToolResult result = tool.execute(inv, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: prompt"));
    }

    @Test
    void foregroundDelegatesToOrchestrator() {
        orchestrator.nextResult = new WorkerResult("w1", "Agent completed the task", true, List.of());

        ToolInvocation inv = new ToolInvocation("agent",
                Map.of("name", "my-agent", "prompt", "do this task"));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertEquals("Agent completed the task", result.output());
        assertNotNull(orchestrator.lastTask);
        assertEquals("do this task", orchestrator.lastTask);
    }

    @Test
    void backgroundModeCreatesTask() {
        ToolInvocation inv = new ToolInvocation("agent",
                Map.of("name", "bg-agent", "prompt", "long task", "background", "true"));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("Background agent"));
        assertTrue(result.output().contains("Task ID:"));

        var tasks = taskManager.listBySession("s1");
        assertEquals(1, tasks.size());
        assertTrue(tasks.get(0).description().contains("bg-agent"));
    }

    @Test
    void usesCustomAgentDefinition() {
        CustomAgentDefinition def = new CustomAgentDefinition(
                "code-reviewer", "Reviews code", "You are a code reviewer.",
                "copilot:gpt-4.1", List.of("file_read"), Map.of());
        agentLoader.register(def);

        orchestrator.nextResult = new WorkerResult("w1", "Review done", true, List.of());

        ToolInvocation inv = new ToolInvocation("agent",
                Map.of("name", "code-reviewer", "prompt", "review the PR"));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertEquals("Review done", result.output());
    }

    @Test
    void adHocAgentWhenNoDefinitionFound() {
        orchestrator.nextResult = new WorkerResult("w1", "Ad-hoc result", true, List.of());

        ToolInvocation inv = new ToolInvocation("agent",
                Map.of("name", "undefined-agent", "prompt", "do something"));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertEquals("Ad-hoc result", result.output());
    }

    @Test
    void modelOverrideRespected() {
        orchestrator.nextResult = new WorkerResult("w1", "Done", true, List.of());

        ToolInvocation inv = new ToolInvocation("agent",
                Map.of("name", "test", "prompt", "test", "model", "anthropic:claude-3.5"));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
    }

    static class StubWorkerOrchestrator implements WorkerOrchestrator {
        WorkerResult nextResult = new WorkerResult("stub", "stub output", true, List.of());
        String lastTask;

        @Override
        public WorkerResult delegate(AgentDefinition worker, String task, AgentContext parentContext) {
            lastTask = task;
            return nextResult;
        }
    }
}
