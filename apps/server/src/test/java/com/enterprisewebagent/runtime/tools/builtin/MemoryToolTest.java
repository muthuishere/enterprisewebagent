package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.memory.MemoryFileLoader;
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

class MemoryToolTest {

    private MemoryTool tool;
    private ToolContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        tool = new MemoryTool(new MemoryFileLoader(), tempDir);
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void toolName() {
        assertEquals("memory", tool.toolName());
    }

    @Test
    void missingActionReturnsFalse() {
        var invocation = new ToolInvocation("memory", Map.of());
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }

    @Test
    void unknownActionReturnsFalse() {
        var invocation = new ToolInvocation("memory", Map.of("action", "unknown"));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
        assertTrue(result.output().contains("Unknown action"));
    }

    @Test
    void showReturnsNoFileMessage() {
        var invocation = new ToolInvocation("memory", Map.of("action", "show"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
        assertTrue(result.output().contains("No MEMORY.md found"));
    }

    @Test
    void showReturnsContent() throws IOException {
        Files.writeString(tempDir.resolve("MEMORY.md"), "# Memory\nContent");

        var invocation = new ToolInvocation("memory", Map.of("action", "show"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
        assertTrue(result.output().contains("# Memory"));
    }

    @Test
    void addAppendsToMemory() {
        var invocation = new ToolInvocation("memory",
                Map.of("action", "add", "content", "- New fact"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
        assertTrue(result.output().contains("Entry added"));
        assertTrue(Files.exists(tempDir.resolve("MEMORY.md")));
    }

    @Test
    void addWithoutContentReturnsFalse() {
        var invocation = new ToolInvocation("memory", Map.of("action", "add"));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
    }

    @Test
    void clearResetsMemory() throws IOException {
        Files.writeString(tempDir.resolve("MEMORY.md"), "old content");

        var invocation = new ToolInvocation("memory", Map.of("action", "clear"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());

        String content = Files.readString(tempDir.resolve("MEMORY.md"));
        assertTrue(content.startsWith("# Project Memory"));
    }

    @Test
    void savePersistsContent() {
        var invocation = new ToolInvocation("memory",
                Map.of("action", "save", "content", "# Saved Memory\nContent here"));
        ToolResult result = tool.execute(invocation, context);
        assertTrue(result.success());
        assertTrue(Files.exists(tempDir.resolve("MEMORY.md")));
    }

    @Test
    void saveWithoutContentReturnsFalse() {
        var invocation = new ToolInvocation("memory", Map.of("action", "save"));
        ToolResult result = tool.execute(invocation, context);
        assertFalse(result.success());
    }
}
