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

class GlobToolTest {

    private GlobTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GlobTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void matchesFilesByPattern(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("hello.java"), "class Hello {}");
        Files.writeString(tempDir.resolve("world.java"), "class World {}");
        Files.writeString(tempDir.resolve("readme.md"), "# Readme");

        ToolInvocation invocation = new ToolInvocation("glob",
                Map.of("pattern", "*.java", "path", tempDir.toString()));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("hello.java"));
        assertTrue(result.output().contains("world.java"));
        assertFalse(result.output().contains("readme.md"));
    }

    @Test
    void noMatchesReturnsSuccessWithMessage(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("readme.md"), "# Readme");

        ToolInvocation invocation = new ToolInvocation("glob",
                Map.of("pattern", "*.java", "path", tempDir.toString()));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("No files matched"));
    }

    @Test
    void skipsHiddenDirectories(@TempDir Path tempDir) throws IOException {
        Path hiddenDir = tempDir.resolve(".hidden");
        Files.createDirectories(hiddenDir);
        Files.writeString(hiddenDir.resolve("secret.java"), "class Secret {}");
        Files.writeString(tempDir.resolve("visible.java"), "class Visible {}");

        ToolInvocation invocation = new ToolInvocation("glob",
                Map.of("pattern", "*.java", "path", tempDir.toString()));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("visible.java"));
        assertFalse(result.output().contains("secret.java"));
    }

    @Test
    void missingPatternReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("glob", Map.of());
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }

    @Test
    void invalidPathReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("glob",
                Map.of("pattern", "*.java", "path", "/nonexistent/dir/xyz"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("not a directory"));
    }
}
