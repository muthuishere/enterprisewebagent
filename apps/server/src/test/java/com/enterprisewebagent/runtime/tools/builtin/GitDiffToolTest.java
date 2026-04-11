package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GitDiffToolTest {

    private GitDiffTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GitDiffTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolName() {
        assertEquals("git_diff", tool.toolName());
    }

    @Test
    void executesBasicDiff() {
        var invocation = new ToolInvocation("git_diff", Map.of());
        ToolResult result = tool.execute(invocation, context);
        // In a git repo, this should succeed
        assertTrue(result.success());
    }

    @Test
    void executesStagedDiff() {
        var invocation = new ToolInvocation("git_diff", Map.of("staged", "true"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
    }

    @Test
    void executesPathDiff() {
        var invocation = new ToolInvocation("git_diff", Map.of("path", "README.md"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
    }

    @Test
    void executesCommitDiff() {
        var invocation = new ToolInvocation("git_diff", Map.of("commit", "HEAD~1"));
        ToolResult result = tool.execute(invocation, context);
        // May fail if repo has no commits, that's acceptable
        assertNotNull(result.output());
    }
}
