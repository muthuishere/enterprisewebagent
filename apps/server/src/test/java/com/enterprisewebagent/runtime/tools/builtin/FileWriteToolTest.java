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

class FileWriteToolTest {

    private FileWriteTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new FileWriteTool();
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void writesNewFile(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("output.txt");

        ToolInvocation invocation = new ToolInvocation("file_write",
                Map.of("path", target.toString(), "content", "hello world"));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("File written"));
        assertTrue(result.output().contains("11 bytes"));
        assertEquals("hello world", Files.readString(target));
    }

    @Test
    void createsParentDirectories(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("sub/dir/output.txt");

        ToolInvocation invocation = new ToolInvocation("file_write",
                Map.of("path", target.toString(), "content", "nested content"));
        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(Files.exists(target));
        assertEquals("nested content", Files.readString(target));
    }

    @Test
    void failsIfFileAlreadyExists(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("existing.txt");
        Files.writeString(target, "original");

        ToolInvocation invocation = new ToolInvocation("file_write",
                Map.of("path", target.toString(), "content", "overwrite attempt"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("File already exists"));
        assertEquals("original", Files.readString(target));
    }

    @Test
    void missingPathReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("file_write",
                Map.of("content", "data"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: path"));
    }

    @Test
    void missingContentReturnsFalse() {
        ToolInvocation invocation = new ToolInvocation("file_write",
                Map.of("path", "/some/path.txt"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: content"));
    }

    @Test
    void createDirsFalseFailsWithoutParent(@TempDir Path tempDir) {
        Path target = tempDir.resolve("noparent/file.txt");

        ToolInvocation invocation = new ToolInvocation("file_write",
                Map.of("path", target.toString(), "content", "data", "create_dirs", "false"));
        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
    }
}
