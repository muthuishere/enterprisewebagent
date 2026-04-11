package com.enterprisewebagent.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.nio.file.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Component
public class CodexTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(CodexTokenProvider.class);
    private static final Path DEFAULT_AUTH_FILE = Path.of(System.getProperty("user.home"), ".codex", "auth.json");

    private final Path authFile;
    private volatile String token;
    private volatile long lastModified = 0;

    public CodexTokenProvider() {
        this(DEFAULT_AUTH_FILE);
    }

    // Package-private for testing
    CodexTokenProvider(Path authFile) {
        this.authFile = authFile;
    }

    @PostConstruct
    public void init() { refreshToken(); }

    public String getToken() {
        try {
            if (Files.exists(authFile)) {
                long currentModified = Files.getLastModifiedTime(authFile).toMillis();
                if (currentModified != lastModified) {
                    refreshToken();
                }
            }
        } catch (Exception e) { /* ignore */ }
        return token;
    }

    public boolean isAvailable() { return getToken() != null; }

    private synchronized void refreshToken() {
        try {
            if (Files.exists(authFile)) {
                String content = Files.readString(authFile);
                var mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> authMap = mapper.readValue(content, Map.class);
                String key = (String) authMap.getOrDefault("api_key", authMap.get("token"));
                if (key != null && !key.isBlank()) {
                    token = key;
                    lastModified = Files.getLastModifiedTime(authFile).toMillis();
                    log.info("Codex token loaded from {}", authFile);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to load Codex token: {}", e.getMessage());
        }
    }
}
