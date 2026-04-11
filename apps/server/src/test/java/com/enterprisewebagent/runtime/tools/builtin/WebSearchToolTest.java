package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchToolTest {

    private WebSearchTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new WebSearchTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolNameIsCorrect() {
        assertEquals("web_search", tool.toolName());
    }

    @Test
    void missingQueryReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("web_search", Map.of());
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: query"));
    }

    @Test
    void blankQueryReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("web_search",
                Map.of("query", "   "));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: query"));
    }

    @Test
    void maxResultsClampsToValidRange() {
        // max_results of -1 should be clamped to 1 (no crash)
        ToolInvocation invocation = new ToolInvocation("web_search",
                Map.of("query", "test query", "max_results", "-1"));
        // This will attempt a real HTTP call; we just verify it doesn't throw
        ToolResult result = tool.execute(invocation, context);
        assertNotNull(result);
    }

    @Test
    void invalidMaxResultsUsesDefault() {
        ToolInvocation invocation = new ToolInvocation("web_search",
                Map.of("query", "test query", "max_results", "not_a_number"));
        ToolResult result = tool.execute(invocation, context);
        assertNotNull(result);
    }
}
