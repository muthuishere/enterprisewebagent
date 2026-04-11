package com.enterprisewebagent.runtime.tools.mcp;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class McpToolExecutorTest {

    @Test
    void executeMapsArgumentsAndReturnsResult() {
        McpSyncClient mockClient = mock(McpSyncClient.class);
        ArgumentCaptor<CallToolRequest> requestCaptor = ArgumentCaptor.forClass(CallToolRequest.class);

        CallToolResult mcpResult = CallToolResult.builder()
                .content(List.of(new TextContent("file contents here")))
                .isError(false)
                .build();

        when(mockClient.callTool(requestCaptor.capture())).thenReturn(mcpResult);

        McpToolExecutor executor = new McpToolExecutor("fs_read_file", "read_file", mockClient);
        assertEquals("fs_read_file", executor.toolName());

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("fs_read_file", Map.of("path", "/tmp/test.txt"));
        ToolResult result = executor.execute(invocation, ctx);

        assertTrue(result.success());
        assertEquals("fs_read_file", result.name());
        assertEquals("file contents here", result.output());

        CallToolRequest captured = requestCaptor.getValue();
        assertEquals("read_file", captured.name());
        assertEquals("/tmp/test.txt", captured.arguments().get("path"));
    }

    @Test
    void executeHandlesErrorResponse() {
        McpSyncClient mockClient = mock(McpSyncClient.class);

        CallToolResult errorResult = CallToolResult.builder()
                .content(List.of(new TextContent("Permission denied")))
                .isError(true)
                .build();

        when(mockClient.callTool(any())).thenReturn(errorResult);

        McpToolExecutor executor = new McpToolExecutor("fs_write", "write", mockClient);
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("fs_write", Map.of("path", "/etc/passwd"));
        ToolResult result = executor.execute(invocation, ctx);

        assertFalse(result.success());
        assertEquals("Permission denied", result.output());
    }

    @Test
    void executeHandlesClientException() {
        McpSyncClient mockClient = mock(McpSyncClient.class);
        when(mockClient.callTool(any())).thenThrow(new RuntimeException("Connection lost"));

        McpToolExecutor executor = new McpToolExecutor("srv_tool", "tool", mockClient);
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("srv_tool", Map.of());
        ToolResult result = executor.execute(invocation, ctx);

        assertFalse(result.success());
        assertTrue(result.output().contains("Connection lost"));
    }

    @Test
    void executeHandlesNullArguments() {
        McpSyncClient mockClient = mock(McpSyncClient.class);
        ArgumentCaptor<CallToolRequest> requestCaptor = ArgumentCaptor.forClass(CallToolRequest.class);

        CallToolResult mcpResult = CallToolResult.builder()
                .content(List.of(new TextContent("ok")))
                .isError(false)
                .build();

        when(mockClient.callTool(requestCaptor.capture())).thenReturn(mcpResult);

        McpToolExecutor executor = new McpToolExecutor("srv_ping", "ping", mockClient);
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("srv_ping", null);
        ToolResult result = executor.execute(invocation, ctx);

        assertTrue(result.success());
        assertTrue(requestCaptor.getValue().arguments().isEmpty());
    }

    @Test
    void executeHandlesEmptyContent() {
        McpSyncClient mockClient = mock(McpSyncClient.class);

        CallToolResult emptyResult = CallToolResult.builder()
                .content(List.of())
                .isError(false)
                .build();

        when(mockClient.callTool(any())).thenReturn(emptyResult);

        McpToolExecutor executor = new McpToolExecutor("srv_noop", "noop", mockClient);
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("srv_noop", Map.of());
        ToolResult result = executor.execute(invocation, ctx);

        assertTrue(result.success());
        assertEquals("", result.output());
    }

    @Test
    void executeHandlesMultipleTextContentParts() {
        McpSyncClient mockClient = mock(McpSyncClient.class);

        CallToolResult multiResult = CallToolResult.builder()
                .content(List.of(
                        new TextContent("line 1"),
                        new TextContent("line 2"),
                        new TextContent("line 3")))
                .isError(false)
                .build();

        when(mockClient.callTool(any())).thenReturn(multiResult);

        McpToolExecutor executor = new McpToolExecutor("srv_multi", "multi", mockClient);
        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        ToolInvocation invocation = new ToolInvocation("srv_multi", Map.of());
        ToolResult result = executor.execute(invocation, ctx);

        assertTrue(result.success());
        assertEquals("line 1\nline 2\nline 3", result.output());
    }
}
