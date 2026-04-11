package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GrepToolTest {

    private GrepTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GrepTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void matchesRegexInFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("test.java"), "public class Hello {\n    int count = 42;\n}");

        ToolInvocation invocation = new ToolInvocation("grep",
                Map.of("pattern", "count", "path", tempDir.toString()));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("test.java:2:"));
        assertTrue(result.output().contains("count = 42"));
    }

    @Test
    void includeFilterLimitsToMatchingFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("app.java"), "// TODO fix this");
        Files.writeString(tempDir.resolve("readme.md"), "// TODO fix this");

        ToolInvocation invocation = new ToolInvocation("grep",
                Map.of("pattern", "TODO", "path", tempDir.toString(), "include", "*.java"));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("app.java"));
        assertFalse(result.output().contains("readme.md"));
    }

    @Test
    void noMatchesReturnsSuccessWithMessage(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("test.java"), "public class Hello {}");

        ToolInvocation invocation = new ToolInvocation("grep",
                Map.of("pattern", "nonexistent_xyz", "path", tempDir.toString()));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("No matches found"));
    }

    @Test
    void skipsHiddenDirectories(@TempDir Path tempDir) throws IOException {
        Path hiddenDir = tempDir.resolve(".hidden");
        Files.createDirectories(hiddenDir);
        Files.writeString(hiddenDir.resolve("secret.txt"), "password=abc123");
        Files.writeString(tempDir.resolve("visible.txt"), "password=abc123");

        ToolInvocation invocation = new ToolInvocation("grep",
                Map.of("pattern", "password", "path", tempDir.toString()));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("visible.txt"));
        assertFalse(result.output().contains("secret.txt"));
    }

    @Test
    void missingPatternReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("grep", Map.of());
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }

    @Test
    void invalidRegexReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("grep",
                Map.of("pattern", "[invalid"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Invalid regex"));
    }
}
