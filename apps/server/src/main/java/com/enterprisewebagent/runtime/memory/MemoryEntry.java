package com.enterprisewebagent.runtime.memory;

import java.time.Instant;

public record MemoryEntry(String key, String content, Instant created, Instant lastAccessed) {
}
