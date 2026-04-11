package com.enterprisewebagent.runtime.agents.custom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomAgentLoaderTest {

    @TempDir
    Path tempDir;

    private CustomAgentLoader loader;

    @BeforeEach
    void setUp() {
        loader = new CustomAgentLoader(List.of(tempDir));
    }

    @Test
    void loadsValidAgentDefinition() throws IOException {
        String json = """
                {
                    "name": "code-reviewer",
                    "description": "Reviews code changes",
                    "systemPrompt": "You are a code reviewer.",
                    "model": "copilot:gpt-4.1",
                    "allowedTools": ["file_read", "grep"]
                }
                """;
        Files.writeString(tempDir.resolve("code-reviewer.json"), json);

        loader.loadAll();

        var def = loader.get("code-reviewer");
        assertTrue(def.isPresent());
        assertEquals("code-reviewer", def.get().name());
        assertEquals("Reviews code changes", def.get().description());
        assertEquals("You are a code reviewer.", def.get().systemPrompt());
        assertEquals("copilot:gpt-4.1", def.get().model());
        assertEquals(List.of("file_read", "grep"), def.get().allowedTools());
    }

    @Test
    void loadsMinimalDefinition() throws IOException {
        String json = """
                {
                    "name": "minimal-agent"
                }
                """;
        Files.writeString(tempDir.resolve("minimal.json"), json);

        loader.loadAll();

        var def = loader.get("minimal-agent");
        assertTrue(def.isPresent());
        assertEquals("minimal-agent", def.get().name());
        assertEquals("", def.get().description());
        assertEquals("copilot:gpt-4.1", def.get().model());
        assertTrue(def.get().allowedTools().isEmpty());
    }

    @Test
    void skipsInvalidJsonFiles() throws IOException {
        Files.writeString(tempDir.resolve("bad.json"), "{ not valid json }}}");
        Files.writeString(tempDir.resolve("good.json"), """
                { "name": "good-agent", "description": "works" }
                """);

        loader.loadAll();

        assertTrue(loader.get("good-agent").isPresent());
        assertEquals(1, loader.listAll().size());
    }

    @Test
    void skipsMissingNameField() throws IOException {
        String json = """
                {
                    "description": "no name"
                }
                """;
        Files.writeString(tempDir.resolve("noname.json"), json);

        loader.loadAll();
        assertTrue(loader.listAll().isEmpty());
    }

    @Test
    void loadsConfigMap() throws IOException {
        String json = """
                {
                    "name": "configured-agent",
                    "config": {
                        "timeout": "30",
                        "verbose": "true"
                    }
                }
                """;
        Files.writeString(tempDir.resolve("configured.json"), json);

        loader.loadAll();

        var def = loader.get("configured-agent");
        assertTrue(def.isPresent());
        assertEquals("30", def.get().config().get("timeout"));
        assertEquals("true", def.get().config().get("verbose"));
    }

    @Test
    void emptyDirectoryLoadsNothing() {
        loader.loadAll();
        assertTrue(loader.listAll().isEmpty());
    }

    @Test
    void nonExistentDirectoryDoesNotThrow() {
        var loaderBad = new CustomAgentLoader(List.of(Path.of("/non/existent/path")));
        assertDoesNotThrow(loaderBad::loadAll);
        assertTrue(loaderBad.listAll().isEmpty());
    }

    @Test
    void reloadClearsOldEntries() throws IOException {
        Files.writeString(tempDir.resolve("first.json"), """
                { "name": "first-agent" }
                """);
        loader.loadAll();
        assertEquals(1, loader.listAll().size());

        Files.delete(tempDir.resolve("first.json"));
        loader.loadAll();
        assertTrue(loader.listAll().isEmpty());
    }

    @Test
    void registerAddsDirectly() {
        var def = new CustomAgentDefinition(
                "manual-agent", "Manually registered", "prompt", "gpt-4.1", List.of(), java.util.Map.of());
        loader.register(def);

        assertTrue(loader.get("manual-agent").isPresent());
    }
}
