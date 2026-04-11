package com.enterprisewebagent.runtime.memory;

import java.util.List;
import java.util.Optional;

public interface MemoryStore {
    void store(MemoryEntry entry);
    Optional<MemoryEntry> retrieve(String key);
    List<MemoryEntry> retrieveRecent(int limit);
}
