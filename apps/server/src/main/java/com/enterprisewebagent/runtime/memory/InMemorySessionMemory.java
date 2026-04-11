package com.enterprisewebagent.runtime.memory;

import com.enterprisewebagent.runtime.query.TurnResult;

import java.time.Instant;

public class InMemorySessionMemory implements SessionMemory {

    private final String sessionId;
    private final MemoryStore memoryStore;

    public InMemorySessionMemory(String sessionId, MemoryStore memoryStore) {
        this.sessionId = sessionId;
        this.memoryStore = memoryStore;
    }

    @Override
    public String getMemoryPrompt() {
        var recent = memoryStore.retrieveRecent(20);
        if (recent.isEmpty()) return "";

        var sb = new StringBuilder("## Session Memory\n\n");
        for (var entry : recent) {
            sb.append("- ").append(entry.key()).append(": ").append(entry.content()).append('\n');
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
}
