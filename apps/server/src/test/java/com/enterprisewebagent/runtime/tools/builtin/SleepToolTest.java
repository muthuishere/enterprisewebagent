package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SleepToolTest {

    private SleepTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new SleepTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void sleepsForSpecifiedDuration() {
        long start = System.currentTimeMillis();

        ToolInvocation invocation = new ToolInvocation("sleep",
                Map.of("seconds", "0.1"));
        ToolResult result = tool.execute(invocation, context);

        long elapsed = System.currentTimeMillis() - start;

        assertTrue(result.success());
        assertTrue(result.output().contains("Slept for"));
        assertTrue(elapsed >= 80, "Should have slept at least ~100ms");
    }

    @Test
    void exceedsMaxLimitReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("sleep",
                Map.of("seconds", "301"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("exceeds maximum"));
    }

    @Test
    void negativeSecondsReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("sleep",
                Map.of("seconds", "-1"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("non-negative"));
    }

    @Test
    void missingSecondsReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("sleep", Map.of());
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }

    @Test
    void invalidSecondsValueReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("sleep",
                Map.of("seconds", "not_a_number"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Invalid seconds"));
    }

    @Test
    void zeroSecondsSucceeds() {
        ToolInvocation invocation = new ToolInvocation("sleep",
                Map.of("seconds", "0"));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("Slept for"));
    }
}
