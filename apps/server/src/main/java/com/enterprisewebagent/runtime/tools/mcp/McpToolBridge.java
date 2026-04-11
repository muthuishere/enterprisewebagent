package com.enterprisewebagent.runtime.tools.mcp;

import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;
import com.enterprisewebagent.runtime.tools.ToolDefinition;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Discovers tools from MCP servers and registers them in the runtime's
 * DefaultToolRegistry. Each discovered tool is namespaced with the server
 * name to avoid collisions with built-in tools.
 */
public class McpToolBridge {

    private static final Logger log = LoggerFactory.getLogger(McpToolBridge.class);

    private final List<NamedMcpClient> namedClients;

    public McpToolBridge(List<NamedMcpClient> namedClients) {
        this.namedClients = namedClients != null ? namedClients : List.of();
    }

    /**
     * Discovers all tools from all configured MCP clients.
     *
     * @return list of ToolDefinitions from all MCP servers
     */
    public List<ToolDefinition> discoverTools() {
        List<ToolDefinition> definitions = new ArrayList<>();
        for (NamedMcpClient named : namedClients) {
            try {
                McpSchema.ListToolsResult result = named.client().listTools();
                if (result.tools() != null) {
                    for (McpSchema.Tool tool : result.tools()) {
                        String prefixed = prefixedName(named.serverName(), tool.name());
                        Map<String, Object> parameters = convertInputSchema(tool.inputSchema());
                        ToolDefinition def = new ToolDefinition(
                                prefixed,
                                tool.description() != null ? tool.description() : prefixed,
                                parameters,
                                false
                        );
                        definitions.add(def);
                    }
                }
                log.info("Discovered {} tools from MCP server '{}'",
                        result.tools() != null ? result.tools().size() : 0, named.serverName());
            } catch (Exception e) {
                log.error("Failed to discover tools from MCP server '{}': {}", named.serverName(), e.getMessage(), e);
            }
        }
        return Collections.unmodifiableList(definitions);
    }

    /**
     * Discovers and registers all MCP tools into the given registry.
     */
    public void registerAll(DefaultToolRegistry registry) {
        for (NamedMcpClient named : namedClients) {
            try {
                McpSchema.ListToolsResult result = named.client().listTools();
                if (result.tools() == null) {
                    continue;
                }
                for (McpSchema.Tool tool : result.tools()) {
                    String prefixed = prefixedName(named.serverName(), tool.name());
                    Map<String, Object> parameters = convertInputSchema(tool.inputSchema());

                    ToolDefinition def = new ToolDefinition(
                            prefixed,
                            tool.description() != null ? tool.description() : prefixed,
                            parameters,
                            false
                    );
                    registry.register(def);

                    McpToolExecutor executor = new McpToolExecutor(prefixed, tool.name(), named.client());
                    registry.registerExecutor(executor);

                    log.debug("Registered MCP tool: {} (from server '{}')", prefixed, named.serverName());
                }
                log.info("Registered {} MCP tools from server '{}'", result.tools().size(), named.serverName());
            } catch (Exception e) {
                log.error("Failed to register tools from MCP server '{}': {}", named.serverName(), e.getMessage(), e);
            }
        }
    }

    static String prefixedName(String serverName, String toolName) {
        String sanitizedServer = sanitize(serverName);
        String sanitizedTool = sanitize(toolName);
        return sanitizedServer + "_" + sanitizedTool;
    }

    private static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertInputSchema(McpSchema.JsonSchema schema) {
        if (schema == null) {
            return Map.of();
        }
        // McpSchema.JsonSchema has type, properties, required, additionalProperties etc.
        // We convert to a simple map representation for the runtime's ToolDefinition.
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (schema.type() != null) {
            result.put("type", schema.type());
        }
        if (schema.properties() != null) {
            result.put("properties", schema.properties());
        }
        if (schema.required() != null) {
            result.put("required", schema.required());
        }
        if (schema.additionalProperties() != null) {
            result.put("additionalProperties", schema.additionalProperties());
        }
        return result;
    }

    /**
     * Pair of server name and McpSyncClient.
     */
    public record NamedMcpClient(String serverName, McpSyncClient client) {}
}
