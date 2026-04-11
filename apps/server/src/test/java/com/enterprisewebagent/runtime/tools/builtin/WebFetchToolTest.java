package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WebFetchToolTest {

    private WebFetchTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new WebFetchTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolNameIsCorrect() {
        assertEquals("web_fetch", tool.toolName());
    }

    @Test
    void missingUrlReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("web_fetch", Map.of());
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: url"));
    }

    @Test
    void invalidUrlSchemeReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("web_fetch",
                Map.of("url", "ftp://example.com"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Invalid URL"));
    }

    @Test
    void blankUrlReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("web_fetch",
                Map.of("url", "   "));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: url"));
    }

    @Test
    void invalidMaxLengthUsesDefault() {
        // Won't crash with invalid max_length
        ToolInvocation invocation = new ToolInvocation("web_fetch",
                Map.of("url", "https://example.com", "max_length", "not_a_number"));
        ToolResult result = tool.execute(invocation, context);
        assertNotNull(result);
    }

    @Test
    void fetchUnreachableHostReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("web_fetch",
                Map.of("url", "http://192.0.2.1:9999/nonexistent"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
    }
}
