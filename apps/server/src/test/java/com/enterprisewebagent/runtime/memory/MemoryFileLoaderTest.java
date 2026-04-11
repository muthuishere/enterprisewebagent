package com.enterprisewebagent.runtime.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MemoryFileLoaderTest {

    private MemoryFileLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new MemoryFileLoader();
    }

    @Test
    void loadProjectMemory_returnsEmptyWhenNoFile() {
        var result = loader.loadProjectMemory(tempDir);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadProjectMemory_loadsFromMemoryMd() throws IOException {
        Files.writeString(tempDir.resolve("MEMORY.md"), "# Memory\nSome content");

        var result = loader.loadProjectMemory(tempDir);
        assertTrue(result.isPresent());
        assertEquals("# Memory\nSome content", result.get());
    }

    @Test
    void loadProjectMemory_loadsFromAgentDirectory() throws IOException {
        Files.createDirectories(tempDir.resolve(".agent"));
        Files.writeString(tempDir.resolve(".agent/MEMORY.md"), "Agent memory");

        var result = loader.loadProjectMemory(tempDir);
        assertTrue(result.isPresent());
        assertEquals("Agent memory", result.get());
    }

    @Test
    void loadProjectMemory_loadsFromClaudeDirectory() throws IOException {
        Files.createDirectories(tempDir.resolve(".claude"));
        Files.writeString(tempDir.resolve(".claude/MEMORY.md"), "Claude memory");

        var result = loader.loadProjectMemory(tempDir);
        assertTrue(result.isPresent());
        assertEquals("Claude memory", result.get());
    }

    @Test
    void loadProjectMemory_prefersRootMemoryMd() throws IOException {
        Files.writeString(tempDir.resolve("MEMORY.md"), "Root memory");
        Files.createDirectories(tempDir.resolve(".agent"));
        Files.writeString(tempDir.resolve(".agent/MEMORY.md"), "Agent memory");

        var result = loader.loadProjectMemory(tempDir);
        assertTrue(result.isPresent());
        assertEquals("Root memory", result.get());
    }

    @Test
    void loadProjectMemory_truncatesLargeFiles() throws IOException {
        String large = "x".repeat(20_000);
        Files.writeString(tempDir.resolve("MEMORY.md"), large);

        var result = loader.loadProjectMemory(tempDir);
        assertTrue(result.isPresent());
        assertEquals(10_240, result.get().length());
    }

    @Test
    void saveProjectMemory_createsFile() throws IOException {
        loader.saveProjectMemory(tempDir, "# New Memory\nContent here");

        assertTrue(Files.exists(tempDir.resolve("MEMORY.md")));
        assertEquals("# New Memory\nContent here", Files.readString(tempDir.resolve("MEMORY.md")));
    }

    @Test
    void appendToMemory_appendsToExistingFile() throws IOException {
        Files.writeString(tempDir.resolve("MEMORY.md"), "# Memory\n");

        loader.appendToMemory(tempDir, "- New entry");

        String content = Files.readString(tempDir.resolve("MEMORY.md"));
        assertTrue(content.contains("# Memory"));
        assertTrue(content.contains("- New entry"));
    }

    @Test
    void appendToMemory_createsFileIfNotExists() throws IOException {
        loader.appendToMemory(tempDir, "- First entry");

        assertTrue(Files.exists(tempDir.resolve("MEMORY.md")));
        String content = Files.readString(tempDir.resolve("MEMORY.md"));
        assertTrue(content.contains("- First entry"));
    }
}
