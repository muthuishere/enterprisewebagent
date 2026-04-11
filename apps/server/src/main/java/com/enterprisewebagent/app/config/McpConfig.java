package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;
import com.enterprisewebagent.runtime.tools.mcp.McpToolBridge;
import com.enterprisewebagent.runtime.tools.mcp.McpToolBridge.NamedMcpClient;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures MCP client connections and bridges MCP tools into the runtime's
 * ToolRegistry. Only active when app.mcp.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "app.mcp.enabled", havingValue = "true")
@EnableConfigurationProperties(McpProperties.class)
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    private final List<McpSyncClient> managedClients = new ArrayList<>();

    @Bean
    public McpToolBridge mcpToolBridge(McpProperties properties, DefaultToolRegistry toolRegistry) {
        List<NamedMcpClient> namedClients = new ArrayList<>();

        for (McpProperties.McpServerConfig server : properties.servers()) {
            try {
                McpSyncClient client = createClient(server);
                client.initialize();
                managedClients.add(client);
                namedClients.add(new NamedMcpClient(server.name(), client));
                log.info("Connected to MCP server '{}'", server.name());
            } catch (Exception e) {
                log.error("Failed to connect to MCP server '{}': {}", server.name(), e.getMessage(), e);
            }
        }

        McpToolBridge bridge = new McpToolBridge(namedClients);
        bridge.registerAll(toolRegistry);
        return bridge;
    }

    private McpSyncClient createClient(McpProperties.McpServerConfig server) {
        McpSchema.Implementation clientInfo = new McpSchema.Implementation(
                "enterprisewebagent-mcp-" + server.name(), "1.0.0");

        if (server.isStdio()) {
            ServerParameters params = ServerParameters.builder(server.command())
                    .args(server.args() != null ? server.args() : List.of())
                    .env(server.env())
                    .build();
            McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.shared());
            StdioClientTransport transport = new StdioClientTransport(params, jsonMapper);
            return McpClient.sync(transport)
                    .clientInfo(clientInfo)
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();
        } else if (server.isSse()) {
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(server.url())
                    .build();
            return McpClient.sync(transport)
                    .clientInfo(clientInfo)
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();
        } else {
            throw new IllegalArgumentException(
                    "MCP server '" + server.name() + "' must have either 'command' (stdio) or 'url' (SSE) configured");
        }
    }

    @PreDestroy
    public void shutdown() {
        for (McpSyncClient client : managedClients) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing MCP client: {}", e.getMessage());
            }
        }
    }
}
