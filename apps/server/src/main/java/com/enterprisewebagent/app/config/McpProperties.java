package com.enterprisewebagent.app.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mcp")
public record McpProperties(boolean enabled, List<McpServerConfig> servers) {

    public McpProperties {
        if (servers == null) {
            servers = List.of();
        }
    }

    public record McpServerConfig(
            String name,
            String command,
            List<String> args,
            Map<String, String> env,
            String url) {

        public boolean isStdio() {
            return command != null && !command.isBlank();
        }

        public boolean isSse() {
            return url != null && !url.isBlank();
        }
    }
}
