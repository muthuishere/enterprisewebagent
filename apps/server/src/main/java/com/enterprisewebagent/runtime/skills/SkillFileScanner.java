package com.enterprisewebagent.runtime.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class SkillFileScanner {

    public static List<Path> scan(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".md"))
                .sorted()
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public static List<Path> scanRecursive(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.walk(directory)) {
            return entries
                .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".md"))
                .sorted()
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
