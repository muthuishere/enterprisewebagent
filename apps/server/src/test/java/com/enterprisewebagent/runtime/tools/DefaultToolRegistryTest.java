package com.enterprisewebagent.runtime.tools;

import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import com.enterprisewebagent.runtime.tools.builtin.BuiltInToolRegistrar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultToolRegistryTest {

    private DefaultToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultToolRegistry();
        BuiltInToolRegistrar.registerAll(registry, new InMemoryEventPublisher());
    }

    @Test
    void registerAndResolveTools() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> tools = registry.resolveTools(ctx);
        assertFalse(tools.isEmpty());

        List<String> names = tools.stream().map(ToolDefinition::name).toList();
        assertTrue(names.contains("file_read"));
        assertTrue(names.contains("file_edit"));
        assertTrue(names.contains("shell"));
        assertTrue(names.contains("ask_user"));
        assertTrue(names.contains("task_stop"));
        assertTrue(names.contains("worker_delegate"));
    }

    @Test
    void denyRuleFiltersDeniedTools() {
        registry.deny("shell");
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> tools = registry.resolveTools(ctx);

        List<String> names = tools.stream().map(ToolDefinition::name).toList();
        assertFalse(names.contains("shell"));
        assertTrue(names.contains("file_read"));
    }

    @Test
    void simpleModeReturnsReducedSet() {
        ToolContext ctx = new ToolContext("s1", "SIMPLE", Set.of());
        List<ToolDefinition> tools = registry.resolveTools(ctx);

        List<String> names = tools.stream().map(ToolDefinition::name).toList();
        assertEquals(4, names.size());
        assertTrue(names.contains("file_read"));
        assertTrue(names.contains("file_edit"));
        assertTrue(names.contains("shell"));
        assertTrue(names.contains("ask_user"));
        assertFalse(names.contains("task_stop"));
        assertFalse(names.contains("worker_delegate"));
    }

    @Test
    void workerModeRespectsCapabilities() {
        ToolContext ctx = new ToolContext("s1", "WORKER", Set.of("file_read", "shell"));
        List<ToolDefinition> tools = registry.resolveTools(ctx);

        List<String> names = tools.stream().map(ToolDefinition::name).toList();
        assertEquals(2, names.size());
        assertTrue(names.contains("file_read"));
        assertTrue(names.contains("shell"));
    }

    @Test
    void workerModeWithEmptyCapabilitiesReturnsNothing() {
        ToolContext ctx = new ToolContext("s1", "WORKER", Set.of());
        List<ToolDefinition> tools = registry.resolveTools(ctx);
        assertTrue(tools.isEmpty());
    }

    @Test
    void deterministicOrdering() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> tools = registry.resolveTools(ctx);
        List<String> names = tools.stream().map(ToolDefinition::name).toList();

        List<String> sorted = names.stream().sorted().toList();
        assertEquals(sorted, names);
    }

    @Test
    void executeDispatchesToCorrectExecutor() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("ask_user",
                Map.of("question", "What is your name?"));
        ToolResult result = registry.execute(invocation, ctx);

        assertTrue(result.success());
        assertTrue(result.output().contains("PENDING_USER_INPUT"));
        assertEquals("ask_user", result.name());
    }

    @Test
    void executeUnknownToolReturnsFalse() {
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("nonexistent", Map.of());
        ToolResult result = registry.execute(invocation, ctx);

        assertFalse(result.success());
        assertTrue(result.output().contains("Unknown tool"));
    }

    @Test
    void getExecutorReturnsPresent() {
        Optional<ToolExecutor> executor = registry.getExecutor("file_read");
        assertTrue(executor.isPresent());
        assertEquals("file_read", executor.get().toolName());
    }

    @Test
    void getExecutorReturnsEmptyForUnknown() {
        Optional<ToolExecutor> executor = registry.getExecutor("nonexistent");
        assertTrue(executor.isEmpty());
    }

    @Test
    void coordinatorModeReturnsAllTools() {
        ToolContext ctx = new ToolContext("s1", "COORDINATOR", Set.of());
        List<ToolDefinition> tools = registry.resolveTools(ctx);
        assertEquals(6, tools.size());
    }

    @Test
    void nullModeDefaultsToNormal() {
        ToolContext ctx = new ToolContext("s1", null, Set.of());
        List<ToolDefinition> tools = registry.resolveTools(ctx);
        assertEquals(6, tools.size());
    }
}
