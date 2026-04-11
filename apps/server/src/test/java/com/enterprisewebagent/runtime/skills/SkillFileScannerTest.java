package com.enterprisewebagent.runtime.skills;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillFileScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void scanReturnsSortedMdFiles() throws IOException {
        Files.writeString(tempDir.resolve("zebra.md"), "z");
        Files.writeString(tempDir.resolve("alpha.md"), "a");
        Files.writeString(tempDir.resolve("middle.md"), "m");

        List<Path> result = SkillFileScanner.scan(tempDir);

        assertEquals(3, result.size());
        assertEquals("alpha.md", result.get(0).getFileName().toString());
        assertEquals("middle.md", result.get(1).getFileName().toString());
        assertEquals("zebra.md", result.get(2).getFileName().toString());
    }

    @Test
    void scanEmptyDirectory() {
        List<Path> result = SkillFileScanner.scan(tempDir);
        assertTrue(result.isEmpty());
    }

    @Test
    void scanNonExistentDirectory() {
        Path missing = tempDir.resolve("does-not-exist");
        List<Path> result = SkillFileScanner.scan(missing);
        assertTrue(result.isEmpty());
    }

    @Test
    void scanExcludesNonMdFiles() throws IOException {
        Files.writeString(tempDir.resolve("skill.md"), "content");
        Files.writeString(tempDir.resolve("readme.txt"), "text");
        Files.writeString(tempDir.resolve("config.yaml"), "yaml");

        List<Path> result = SkillFileScanner.scan(tempDir);

        assertEquals(1, result.size());
        assertEquals("skill.md", result.get(0).getFileName().toString());
    }

    @Test
    void scanDoesNotRecurse() throws IOException {
        Files.writeString(tempDir.resolve("top.md"), "top");
        Path subDir = Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(subDir.resolve("nested.md"), "nested");

        List<Path> result = SkillFileScanner.scan(tempDir);

        assertEquals(1, result.size());
        assertEquals("top.md", result.get(0).getFileName().toString());
    }

    @Test
    void scanRecursiveIncludesSubdirectories() throws IOException {
        Files.writeString(tempDir.resolve("top.md"), "top");
        Path subDir = Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(subDir.resolve("nested.md"), "nested");

        List<Path> result = SkillFileScanner.scanRecursive(tempDir);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.getFileName().toString().equals("top.md")));
        assertTrue(result.stream().anyMatch(p -> p.getFileName().toString().equals("nested.md")));
    }

    @Test
    void scanNullDirectory() {
        List<Path> result = SkillFileScanner.scan(null);
        assertTrue(result.isEmpty());
    }
}
