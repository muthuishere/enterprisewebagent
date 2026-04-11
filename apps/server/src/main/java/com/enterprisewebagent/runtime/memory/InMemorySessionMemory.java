package com.enterprisewebagent.runtime.memory;

import com.enterprisewebagent.runtime.query.TurnResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemorySessionMemory implements SessionMemory {

    private final String sessionId;
    private final MemoryStore memoryStore;
    private final MemoryFileLoader memoryFileLoader;
    private final List<String> decisions = Collections.synchronizedList(new ArrayList<>());
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final List<String> fileChanges = Collections.synchronizedList(new ArrayList<>());
    private String projectMemory;

    public InMemorySessionMemory(String sessionId, MemoryStore memoryStore) {
        this(sessionId, memoryStore, null, null);
    }

    public InMemorySessionMemory(String sessionId, MemoryStore memoryStore,
                                 MemoryFileLoader memoryFileLoader, Path projectRoot) {
        this.sessionId = sessionId;
        this.memoryStore = memoryStore;
        this.memoryFileLoader = memoryFileLoader;

        if (memoryFileLoader != null && projectRoot != null) {
            this.projectMemory = memoryFileLoader.loadProjectMemory(projectRoot).orElse(null);
        }
    }

    @Override
    public String getMemoryPrompt() {
        var sb = new StringBuilder();

        if (projectMemory != null && !projectMemory.isBlank()) {
            sb.append("## Project Memory\n\n").append(projectMemory).append("\n\n");
        }

        var recent = memoryStore.retrieveRecent(20);
        if (!recent.isEmpty()) {
            sb.append("## Session Memory\n\n");
            for (var entry : recent) {
                sb.append("- ").append(entry.key()).append(": ").append(entry.content()).append('\n');
            }
        }

        if (!decisions.isEmpty()) {
            sb.append("\n### Session Decisions\n");
            synchronized (decisions) {
                for (var d : decisions) {
                    sb.append("- ").append(d).append('\n');
                }
            }
        }

        if (!errors.isEmpty()) {
            sb.append("\n### Session Errors\n");
            synchronized (errors) {
                for (var e : errors) {
                    sb.append("- ").append(e).append('\n');
                }
            }
        }

        if (!fileChanges.isEmpty()) {
            sb.append("\n### Session File Changes\n");
            synchronized (fileChanges) {
                for (var f : fileChanges) {
                    sb.append("- ").append(f).append('\n');
                }
            }
        }

        return sb.toString();
    }

    @Override
    public void onTurnComplete(TurnResult result) {
        if (result.output() == null || result.output().isBlank()) return;

        var key = "turn_" + Instant.now().toEpochMilli();
        var content = result.output().length() > 200
                ? result.output().substring(0, 200)
                : result.output();
        var now = Instant.now();
        memoryStore.store(new MemoryEntry(key, content, now, now));
    }

    public void trackDecision(String decision) {
        decisions.add(decision);
    }

    public void trackError(String error) {
        errors.add(error);
    }

    public void trackFileChange(String fileChange) {
        fileChanges.add(fileChange);
    }

    public List<String> getDecisions() {
        return List.copyOf(decisions);
    }

    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    public List<String> getFileChanges() {
        return List.copyOf(fileChanges);
    }

    public String getProjectMemory() {
        return projectMemory;
    }
}
