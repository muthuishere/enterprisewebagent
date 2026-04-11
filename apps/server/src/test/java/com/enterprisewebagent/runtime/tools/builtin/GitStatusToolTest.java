package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GitStatusToolTest {

    private GitStatusTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GitStatusTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolName() {
        assertEquals("git_status", tool.toolName());
    }

    @Test
    void executesStatus() {
        var invocation = new ToolInvocation("git_status", Map.of());
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
        assertNotNull(result.output());
    }
}
