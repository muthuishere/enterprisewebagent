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

class FileEditToolTest {

    private FileEditTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new FileEditTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void successfulEdit(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        ToolInvocation invocation = new ToolInvocation("file_edit",
                Map.of("path", file.toString(), "old_str", "hello", "new_str", "goodbye"));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertEquals("goodbye world", Files.readString(file));
    }

    @Test
    void oldStrNotFoundReturnsFalse(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        ToolInvocation invocation = new ToolInvocation("file_edit",
                Map.of("path", file.toString(), "old_str", "nonexistent", "new_str", "replacement"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("old_str not found"));
    }

    @Test
    void ambiguousOldStrReturnsFalse(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello hello world");

        ToolInvocation invocation = new ToolInvocation("file_edit",
                Map.of("path", file.toString(), "old_str", "hello", "new_str", "goodbye"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("ambiguous"));
    }

    @Test
    void editNonExistentFileReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("file_edit",
                Map.of("path", "/nonexistent/file.txt", "old_str", "a", "new_str", "b"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("File not found"));
    }
}
