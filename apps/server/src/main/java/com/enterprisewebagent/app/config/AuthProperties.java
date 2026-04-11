package com.enterprisewebagent.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(boolean enabled, List<String> apiKeys) {

    public boolean isValidKey(String key) {
        if (!enabled) return true;
        if (apiKeys == null || apiKeys.isEmpty()) return true;
        return apiKeys.stream()
                .filter(k -> k != null && !k.isBlank())
                .anyMatch(k -> k.equals(key));
    }
}
