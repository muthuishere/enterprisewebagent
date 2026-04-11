package com.enterprisewebagent.app.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CodexTokenProviderTest {

    @Test
    void isAvailableReturnsFalseWhenAuthFileDoesNotExist(@TempDir Path tempDir) {
        Path nonexistent = tempDir.resolve("nonexistent/auth.json");
        CodexTokenProvider provider = new CodexTokenProvider(nonexistent);
        assertFalse(provider.isAvailable());
    }

    @Test
    void getTokenReturnsNullWhenNoAuthFile(@TempDir Path tempDir) {
        Path nonexistent = tempDir.resolve("does-not-exist.json");
        CodexTokenProvider provider = new CodexTokenProvider(nonexistent);
        assertNull(provider.getToken());
        assertFalse(provider.isAvailable());
    }

    @Test
    void getTokenReadsApiKeyFromJsonFile(@TempDir Path tempDir) throws Exception {
        Path authFile = tempDir.resolve("auth.json");
        Files.writeString(authFile, """
            {"api_key": "test-codex-key-12345"}
            """);

        CodexTokenProvider provider = new CodexTokenProvider(authFile);
        provider.init();

        assertEquals("test-codex-key-12345", provider.getToken());
        assertTrue(provider.isAvailable());
    }

    @Test
    void getTokenReadsTokenFieldFromJsonFile(@TempDir Path tempDir) throws Exception {
        Path authFile = tempDir.resolve("auth.json");
        Files.writeString(authFile, """
            {"token": "test-token-field-67890"}
            """);

        CodexTokenProvider provider = new CodexTokenProvider(authFile);
        provider.init();

        assertEquals("test-token-field-67890", provider.getToken());
        assertTrue(provider.isAvailable());
    }

    @Test
    void getTokenPrefersApiKeyOverTokenField(@TempDir Path tempDir) throws Exception {
        Path authFile = tempDir.resolve("auth.json");
        Files.writeString(authFile, """
            {"api_key": "preferred-key", "token": "fallback-token"}
            """);

        CodexTokenProvider provider = new CodexTokenProvider(authFile);
        provider.init();

        assertEquals("preferred-key", provider.getToken());
    }

    @Test
    void getTokenHandlesMalformedJson(@TempDir Path tempDir) throws Exception {
        Path authFile = tempDir.resolve("auth.json");
        Files.writeString(authFile, "not valid json {{{");

        CodexTokenProvider provider = new CodexTokenProvider(authFile);
        provider.init();

        assertNull(provider.getToken());
        assertFalse(provider.isAvailable());
    }

    @Test
    void getTokenHandlesEmptyApiKey(@TempDir Path tempDir) throws Exception {
        Path authFile = tempDir.resolve("auth.json");
        Files.writeString(authFile, """
            {"api_key": "   "}
            """);

        CodexTokenProvider provider = new CodexTokenProvider(authFile);
        provider.init();

        assertNull(provider.getToken());
        assertFalse(provider.isAvailable());
    }

    @Test
    void getTokenDetectsFileChanges(@TempDir Path tempDir) throws Exception {
        Path authFile = tempDir.resolve("auth.json");
        Files.writeString(authFile, """
            {"api_key": "first-key"}
            """);

        CodexTokenProvider provider = new CodexTokenProvider(authFile);
        provider.init();
        assertEquals("first-key", provider.getToken());

        // Ensure file modification time changes
        Thread.sleep(50);
        Files.writeString(authFile, """
            {"api_key": "second-key"}
            """);

        assertEquals("second-key", provider.getToken());
    }
}
