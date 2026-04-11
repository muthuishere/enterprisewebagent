package com.enterprisewebagent.runtime.tools.mcp;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridges a single MCP tool to the runtime's ToolExecutor interface.
 * Delegates execution to an McpSyncClient, converting between the
 * runtime's ToolInvocation/ToolResult and MCP's CallToolRequest/CallToolResult.
 */
public class McpToolExecutor implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(McpToolExecutor.class);

    private final String prefixedName;
    private final String originalName;
    private final McpSyncClient mcpClient;

    public McpToolExecutor(String prefixedName, String originalName, McpSyncClient mcpClient) {
        this.prefixedName = prefixedName;
        this.originalName = originalName;
        this.mcpClient = mcpClient;
    }

    @Override
    public String toolName() {
        return prefixedName;
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> arguments = invocation.arguments() != null
                ? invocation.arguments()
                : Map.of();

        try {
            CallToolRequest request = CallToolRequest.builder()
                    .name(originalName)
                    .arguments(arguments)
                    .build();

            CallToolResult response = mcpClient.callTool(request);

            String output = extractTextContent(response);

            boolean isError = response.isError() != null && response.isError();
            return new ToolResult(prefixedName, output, !isError);
        } catch (Exception e) {
            log.error("MCP tool execution failed for {}: {}", prefixedName, e.getMessage(), e);
            return new ToolResult(prefixedName, "MCP tool error: " + e.getMessage(), false);
        }
    }

    private String extractTextContent(CallToolResult result) {
        if (result.content() == null || result.content().isEmpty()) {
            return "";
        }
        return result.content().stream()
                .map(content -> {
                    if (content instanceof McpSchema.TextContent text) {
                        return text.text();
                    }
                    return content.toString();
                })
                .collect(Collectors.joining("\n"));
    }
}
