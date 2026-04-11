package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GitCommitToolTest {

    private GitCommitTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GitCommitTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolName() {
        assertEquals("git_commit", tool.toolName());
    }

    @Test
    void missingMessageReturnsFalse() {
        var invocation = new ToolInvocation("git_commit", Map.of());
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: message"));
    }

    @Test
    void blankMessageReturnsFalse() {
        var invocation = new ToolInvocation("git_commit", Map.of("message", "  "));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
    }

    @Test
    void commitWithNoChangesReturnsFalse() {
        var invocation = new ToolInvocation("git_commit", Map.of("message", "test commit"));
        ToolResult result = tool.execute(invocation, context);
        // Should fail because nothing to commit
        assertFalse(result.success());
    }
}
