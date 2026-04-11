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

class FileReadToolTest {

    private FileReadTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new FileReadTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void readExistingFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        ToolInvocation invocation = new ToolInvocation("file_read",
                Map.of("path", file.toString()));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertEquals("hello world", result.output());
    }

    @Test
    void readNonExistentFileReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("file_read",
                Map.of("path", "/nonexistent/path/file.txt"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("File not found"));
    }

    @Test
    void missingPathParameterReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("file_read", Map.of());
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
    }
}
