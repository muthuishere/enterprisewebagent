package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GitLogToolTest {

    private GitLogTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GitLogTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolName() {
        assertEquals("git_log", tool.toolName());
    }

    @Test
    void executesDefaultLog() {
        var invocation = new ToolInvocation("git_log", Map.of());
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
        assertNotNull(result.output());
    }

    @Test
    void executesWithCount() {
        var invocation = new ToolInvocation("git_log", Map.of("count", "5"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
    }

    @Test
    void executesWithPath() {
        var invocation = new ToolInvocation("git_log", Map.of("path", "README.md"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
    }

    @Test
    void executesWithCustomFormat() {
        var invocation = new ToolInvocation("git_log",
                Map.of("format", "%H %s", "count", "3"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
    }

    @Test
    void invalidCountUsesDefault() {
        var invocation = new ToolInvocation("git_log", Map.of("count", "notanumber"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
    }
}
