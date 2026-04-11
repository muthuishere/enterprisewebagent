package com.enterprisewebagent.runtime.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class MemoryFileLoader {

    private static final Logger log = LoggerFactory.getLogger(MemoryFileLoader.class);
    private static final long MAX_SIZE_BYTES = 10 * 1024; // 10KB

    private static final List<String> SEARCH_PATHS = List.of(
            "MEMORY.md",
            ".agent/MEMORY.md",
            ".claude/MEMORY.md"
    );

    public Optional<String> loadProjectMemory(Path projectRoot) {
        for (String relativePath : SEARCH_PATHS) {
            Path candidate = projectRoot.resolve(relativePath);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                try {
                    long size = Files.size(candidate);
                    if (size > MAX_SIZE_BYTES) {
                        log.warn("MEMORY.md exceeds max size ({} bytes > {}), truncating", size, MAX_SIZE_BYTES);
                        byte[] bytes = Files.readAllBytes(candidate);
                        return Optional.of(new String(bytes, 0, (int) MAX_SIZE_BYTES));
                    }
                    return Optional.of(Files.readString(candidate));
                } catch (IOException e) {
                    log.error("Failed to read {}: {}", candidate, e.getMessage());
                }
            }
        }
        return Optional.empty();
    }

    public void saveProjectMemory(Path projectRoot, String content) throws IOException {
        Path target = projectRoot.resolve("MEMORY.md");
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
        log.info("Saved project memory to {}", target);
    }

    public void appendToMemory(Path projectRoot, String entry) throws IOException {
        Path target = resolveExistingOrDefault(projectRoot);
        Files.createDirectories(target.getParent());

        String existing = Files.exists(target) ? Files.readString(target) : "";
        String separator = existing.isEmpty() || existing.endsWith("\n") ? "" : "\n";
        String updated = existing + separator + entry + "\n";

        if (updated.length() > MAX_SIZE_BYTES) {
            log.warn("Append would exceed max size, truncating oldest content");
            updated = updated.substring(updated.length() - (int) MAX_SIZE_BYTES);
            int newlineIdx = updated.indexOf('\n');
            if (newlineIdx > 0) {
                updated = updated.substring(newlineIdx + 1);
            }
        }

        Files.writeString(target, updated);
        log.info("Appended to project memory at {}", target);
    }

    private Path resolveExistingOrDefault(Path projectRoot) {
        for (String relativePath : SEARCH_PATHS) {
            Path candidate = projectRoot.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return projectRoot.resolve("MEMORY.md");
    }
}
