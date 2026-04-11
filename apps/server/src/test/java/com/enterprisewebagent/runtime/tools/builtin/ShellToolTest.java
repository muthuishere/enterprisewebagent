package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShellToolTest {

    private ShellTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new ShellTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void simpleCommandExecution() {
        ToolInvocation invocation = new ToolInvocation("shell",
                Map.of("command", "echo hello"));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertEquals("hello\n", result.output());
    }

    @Test
    void commandFailureReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("shell",
                Map.of("command", "exit 1"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Exit code 1"));
    }

    @Test
    void missingCommandReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("shell", Map.of());
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }
}
