package com.enterprisewebagent.runtime.tools.mcp;

import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.mcp.McpToolBridge.NamedMcpClient;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpToolBridgeTest {

    @Test
    void discoverToolsCreatesNamespacedDefinitions() {
        McpSyncClient mockClient = mock(McpSyncClient.class);

        Tool readFile = Tool.builder()
                .name("read_file")
                .description("Read a file from disk")
                .inputSchema(new JsonSchema(
                        "object",
                        Map.of("path", Map.of("type", "string")),
                        List.of("path"),
                        null, null, null))
                .build();

        Tool listDir = Tool.builder()
                .name("list_directory")
                .description("List directory contents")
                .inputSchema(new JsonSchema(
                        "object",
                        Map.of("path", Map.of("type", "string")),
                        null, null, null, null))
                .build();

        when(mockClient.listTools()).thenReturn(new ListToolsResult(List.of(readFile, listDir), null));

        McpToolBridge bridge = new McpToolBridge(List.of(new NamedMcpClient("filesystem", mockClient)));
        List<ToolDefinition> tools = bridge.discoverTools();

        assertEquals(2, tools.size());

        ToolDefinition first = tools.stream()
                .filter(t -> t.name().equals("filesystem_read_file"))
                .findFirst().orElseThrow();
        assertEquals("Read a file from disk", first.description());
        assertFalse(first.builtIn());
        assertEquals("object", first.parameters().get("type"));

        ToolDefinition second = tools.stream()
                .filter(t -> t.name().equals("filesystem_list_directory"))
                .findFirst().orElseThrow();
        assertEquals("List directory contents", second.description());
    }

    @Test
    void discoverToolsFromMultipleServers() {
        McpSyncClient client1 = mock(McpSyncClient.class);
        McpSyncClient client2 = mock(McpSyncClient.class);

        Tool tool1 = Tool.builder().name("search").description("Search files")
                .inputSchema(new JsonSchema("object", null, null, null, null, null)).build();
        Tool tool2 = Tool.builder().name("search").description("Search GitHub")
                .inputSchema(new JsonSchema("object", null, null, null, null, null)).build();

        when(client1.listTools()).thenReturn(new ListToolsResult(List.of(tool1), null));
        when(client2.listTools()).thenReturn(new ListToolsResult(List.of(tool2), null));

        McpToolBridge bridge = new McpToolBridge(List.of(
                new NamedMcpClient("filesystem", client1),
                new NamedMcpClient("github", client2)
        ));
        List<ToolDefinition> tools = bridge.discoverTools();

        assertEquals(2, tools.size());
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("filesystem_search")));
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("github_search")));
    }

    @Test
    void registerAllAddsToolsToRegistry() {
        McpSyncClient mockClient = mock(McpSyncClient.class);

        Tool tool = Tool.builder()
                .name("read_file")
                .description("Read a file")
                .inputSchema(new JsonSchema("object", null, null, null, null, null))
                .build();

        when(mockClient.listTools()).thenReturn(new ListToolsResult(List.of(tool), null));

        DefaultToolRegistry registry = new DefaultToolRegistry();
        McpToolBridge bridge = new McpToolBridge(List.of(new NamedMcpClient("fs", mockClient)));
        bridge.registerAll(registry);

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        List<ToolDefinition> resolved = registry.resolveTools(ctx);
        assertTrue(resolved.stream().anyMatch(t -> t.name().equals("fs_read_file")));
        assertTrue(registry.getExecutor("fs_read_file").isPresent());
    }

    @Test
    void registerAllHandlesClientError() {
        McpSyncClient failClient = mock(McpSyncClient.class);
        when(failClient.listTools()).thenThrow(new RuntimeException("Connection refused"));

        DefaultToolRegistry registry = new DefaultToolRegistry();
        McpToolBridge bridge = new McpToolBridge(List.of(new NamedMcpClient("broken", failClient)));
        // Should not throw
        bridge.registerAll(registry);

        ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());
        assertTrue(registry.resolveTools(ctx).isEmpty());
    }

    @Test
    void emptyClientListProducesNoTools() {
        McpToolBridge bridge = new McpToolBridge(List.of());
        assertTrue(bridge.discoverTools().isEmpty());
    }

    @Test
    void prefixedNameSanitizesSpecialCharacters() {
        assertEquals("my_server_my_tool", McpToolBridge.prefixedName("my-server", "my-tool"));
        assertEquals("server_read_file", McpToolBridge.prefixedName("server", "read_file"));
        assertEquals("s_1_tool_2", McpToolBridge.prefixedName("s.1", "tool.2"));
    }

    @Test
    void nullInputSchemaProducesEmptyParameters() {
        McpSyncClient mockClient = mock(McpSyncClient.class);

        Tool tool = Tool.builder()
                .name("no_schema")
                .description("Tool without schema")
                .build();

        when(mockClient.listTools()).thenReturn(new ListToolsResult(List.of(tool), null));

        McpToolBridge bridge = new McpToolBridge(List.of(new NamedMcpClient("srv", mockClient)));
        List<ToolDefinition> tools = bridge.discoverTools();

        assertEquals(1, tools.size());
        assertTrue(tools.get(0).parameters().isEmpty());
    }
}
