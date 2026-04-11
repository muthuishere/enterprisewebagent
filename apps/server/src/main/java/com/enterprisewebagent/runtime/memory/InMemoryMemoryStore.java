package com.enterprisewebagent.runtime.memory;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMemoryStore implements MemoryStore {

    private final ConcurrentHashMap<String, MemoryEntry> entries = new ConcurrentHashMap<>();

    @Override
    public void store(MemoryEntry entry) {
        var updated = new MemoryEntry(entry.key(), entry.content(), entry.created(), Instant.now());
        entries.put(entry.key(), updated);
    }

    @Override
    public Optional<MemoryEntry> retrieve(String key) {
        var entry = entries.get(key);
        if (entry == null) return Optional.empty();
        var accessed = new MemoryEntry(entry.key(), entry.content(), entry.created(), Instant.now());
        entries.put(key, accessed);
        return Optional.of(accessed);
    }

    @Override
    public List<MemoryEntry> retrieveRecent(int limit) {
        return entries.values().stream()
                .sorted(Comparator.comparing(MemoryEntry::lastAccessed).reversed())
                .limit(limit)
                .toList();
    }
}
