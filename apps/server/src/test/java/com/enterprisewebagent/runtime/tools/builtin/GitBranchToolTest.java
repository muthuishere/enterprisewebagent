package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GitBranchToolTest {

    private GitBranchTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GitBranchTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolName() {
        assertEquals("git_branch", tool.toolName());
    }

    @Test
    void missingActionReturnsFalse() {
        var invocation = new ToolInvocation("git_branch", Map.of());
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }

    @Test
    void unknownActionReturnsFalse() {
        var invocation = new ToolInvocation("git_branch", Map.of("action", "unknown"));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
    }

    @Test
    void listBranches() {
        var invocation = new ToolInvocation("git_branch", Map.of("action", "list"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
        assertNotNull(result.output());
    }

    @Test
    void createRequiresName() {
        var invocation = new ToolInvocation("git_branch", Map.of("action", "create"));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: name"));
    }

    @Test
    void switchRequiresName() {
        var invocation = new ToolInvocation("git_branch", Map.of("action", "switch"));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: name"));
    }

    @Test
    void deleteRequiresName() {
        var invocation = new ToolInvocation("git_branch", Map.of("action", "delete"));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: name"));
    }
}
