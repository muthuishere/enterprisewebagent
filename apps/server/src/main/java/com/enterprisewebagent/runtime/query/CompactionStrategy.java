package com.enterprisewebagent.runtime.query;

public enum CompactionStrategy {
    CLEAR,      // Hard reset: clear all caches, keep only last N transcript entries
    COMPACT,    // Soft reset: summarize old transcript, clear dynamic cache, keep static cache
    TRUNCATE    // Emergency: keep only last 2 transcript entries, clear all caches
}
