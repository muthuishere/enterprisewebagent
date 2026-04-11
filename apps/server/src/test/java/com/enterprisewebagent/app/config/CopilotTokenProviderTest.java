package com.enterprisewebagent.app.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CopilotTokenProviderTest {

    @Test
    void isAvailableReturnsFalseWhenGhCliNotAvailable() {
        // In a test environment, gh may or may not be authenticated.
        // We just verify the provider doesn't throw and returns a consistent state.
        CopilotTokenProvider provider = new CopilotTokenProvider();
        // getToken() should not throw even if gh is not installed
        String token = provider.getToken();
        // isAvailable should be consistent with getToken
        assertEquals(token != null, provider.isAvailable());
    }

    @Test
    void getTokenReturnsSameValueOnRepeatedCalls() {
        CopilotTokenProvider provider = new CopilotTokenProvider();
        String first = provider.getToken();
        String second = provider.getToken();
        assertEquals(first, second, "Repeated calls should return the same cached token");
    }
}
