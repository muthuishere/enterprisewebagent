package com.enterprisewebagent.runtime.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HookConfigLoaderTest {

    private HookConfigLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new HookConfigLoader(new ObjectMapper());
    }

    @Test
    void loadFromFile_returnsEmptyForMissingFile() {
        var hooks = loader.loadFromFile(tempDir.resolve("nonexistent.json"), "test");
        assertTrue(hooks.isEmpty());
    }

    @Test
    void loadFromFile_parsesValidConfig() throws IOException {
        String json = """
                [
                    {"type": "PRE_TURN", "command": "echo starting", "order": 1},
                    {"type": "POST_TURN", "command": "echo done", "order": 2}
                ]
                """;
        Path configFile = tempDir.resolve("hooks.json");
        Files.writeString(configFile, json);

        List<Hook> hooks = loader.loadFromFile(configFile, "test");
        assertEquals(2, hooks.size());
        assertEquals(HookType.PRE_TURN, hooks.get(0).type());
        assertEquals("echo starting", hooks.get(0).command());
        assertEquals(1, hooks.get(0).order());
        assertTrue(hooks.get(0).enabled());
    }

    @Test
    void loadFromFile_handlesDisabledHooks() throws IOException {
        String json = """
                [
                    {"type": "PRE_TOOL", "command": "echo tool", "enabled": false}
                ]
                """;
        Path configFile = tempDir.resolve("hooks.json");
        Files.writeString(configFile, json);

        List<Hook> hooks = loader.loadFromFile(configFile, "test");
        assertEquals(1, hooks.size());
        assertFalse(hooks.get(0).enabled());
    }

    @Test
    void loadFromFile_skipsMissingCommand() throws IOException {
        String json = """
                [
                    {"type": "PRE_TURN"},
                    {"type": "POST_TURN", "command": "echo valid"}
                ]
                """;
        Path configFile = tempDir.resolve("hooks.json");
        Files.writeString(configFile, json);

        List<Hook> hooks = loader.loadFromFile(configFile, "test");
        assertEquals(1, hooks.size());
    }

    @Test
    void loadFromFile_skipsInvalidType() throws IOException {
        String json = """
                [
                    {"type": "INVALID_TYPE", "command": "echo bad"}
                ]
                """;
        Path configFile = tempDir.resolve("hooks.json");
        Files.writeString(configFile, json);

        List<Hook> hooks = loader.loadFromFile(configFile, "test");
        assertTrue(hooks.isEmpty());
    }

    @Test
    void loadFromFile_handlesCustomId() throws IOException {
        String json = """
                [
                    {"id": "my-hook", "type": "ON_SESSION_START", "command": "echo hello", "order": 5}
                ]
                """;
        Path configFile = tempDir.resolve("hooks.json");
        Files.writeString(configFile, json);

        List<Hook> hooks = loader.loadFromFile(configFile, "test");
        assertEquals(1, hooks.size());
        assertEquals("my-hook", hooks.get(0).id());
        assertEquals(5, hooks.get(0).order());
    }

    @Test
    void loadHooks_loadsProjectHooks() throws IOException {
        Files.createDirectories(tempDir.resolve(".agent"));
        String json = """
                [
                    {"type": "PRE_TURN", "command": "echo project"}
                ]
                """;
        Files.writeString(tempDir.resolve(".agent/hooks.json"), json);

        List<Hook> hooks = loader.loadHooks(tempDir);
        // Should include at least the project hooks
        assertFalse(hooks.isEmpty());
    }
}
