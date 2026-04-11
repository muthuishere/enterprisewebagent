package com.enterprisewebagent.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Component
public class CopilotTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(CopilotTokenProvider.class);
    private static final long REFRESH_INTERVAL_SECONDS = 30 * 60; // 30 min

    private volatile String token;
    private volatile Instant lastRefresh = Instant.EPOCH;

    @PostConstruct
    public void init() { refreshToken(); }

    public String getToken() {
        if (token != null && !isExpired()) return token;
        refreshToken();
        return token;
    }

    public boolean isAvailable() { return getToken() != null; }

    private boolean isExpired() {
        return Instant.now().isAfter(lastRefresh.plusSeconds(REFRESH_INTERVAL_SECONDS));
    }

    private synchronized void refreshToken() {
        try {
            ProcessBuilder pb = new ProcessBuilder("gh", "auth", "token");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            if (exitCode == 0 && !output.isBlank()) {
                token = output;
                lastRefresh = Instant.now();
                log.info("Copilot token refreshed");
            } else {
                log.debug("gh auth token failed exitCode={}", exitCode);
            }
        } catch (Exception e) {
            log.debug("Failed to obtain Copilot token: {}", e.getMessage());
        }
    }
}
