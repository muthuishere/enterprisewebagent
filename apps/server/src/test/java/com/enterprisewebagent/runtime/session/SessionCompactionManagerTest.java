package com.enterprisewebagent.runtime.session;

import com.enterprisewebagent.runtime.prompt.InMemoryPromptSectionCache;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.prompt.PromptSectionCache;
import com.enterprisewebagent.runtime.query.CompactionStrategy;
import com.enterprisewebagent.runtime.query.TranscriptEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionCompactionManagerTest {

    private PromptSectionCache cache;
    private SessionCompactionManager manager;

    @BeforeEach
    void setUp() {
        cache = new InMemoryPromptSectionCache();
        manager = new SessionCompactionManager(cache);
    }

    private static TranscriptEntry entry(String role, String content) {
        return new TranscriptEntry(role, content, Instant.now());
    }

    private static List<TranscriptEntry> generateEntries(int count) {
        var list = new ArrayList<TranscriptEntry>();
        for (int i = 0; i < count; i++) {
            list.add(entry(i % 2 == 0 ? "user" : "assistant", "Message " + i));
        }
        return list;
    }

    @Test
    void compactWithClearStrategyClearsAllCaches() {
        cache.put("static-key", "value");
        cache.put("dynamic-key", "value");
        var entries = generateEntries(30);

        var result = manager.compact(entries, CompactionStrategy.CLEAR);

        assertEquals(10, result.compactedSize());
        assertEquals(30, result.originalSize());
        assertEquals(CompactionStrategy.CLEAR, result.strategyUsed());
        assertTrue(cache.get("static-key").isEmpty());
        assertTrue(cache.get("dynamic-key").isEmpty());
    }

    @Test
    void compactWithCompactStrategyOnlyClearsDynamic() {
        cache.put("some-key", "value");
        var entries = generateEntries(40);

        var result = manager.compact(entries, CompactionStrategy.COMPACT);

        assertEquals(CompactionStrategy.COMPACT, result.strategyUsed());
        // COMPACT calls clearDynamic, which removes dynamic catalog keys
        // Static keys that are not in PromptCatalog dynamic set remain
        assertEquals(21, result.compactedSize());
        assertEquals(40, result.originalSize());
    }

    @Test
    void compactWithTruncateStrategyClearsAllCaches() {
        cache.put("key1", "value1");
        var entries = generateEntries(50);

        var result = manager.compact(entries, CompactionStrategy.TRUNCATE);

        assertEquals(2, result.compactedSize());
        assertEquals(50, result.originalSize());
        assertTrue(cache.get("key1").isEmpty());
    }

    @Test
    void detectNeededReturnsEmptyForSmallSessions() {
        var sections = List.of(new PromptSection("test", "small content", false));
        var transcript = generateEntries(10);

        var result = manager.detectNeeded(sections, transcript);
        assertTrue(result.isEmpty());
    }

    @Test
    void detectNeededReturnsCompactForLongTranscripts() {
        var sections = List.of(new PromptSection("test", "content", false));
        var transcript = generateEntries(201);

        var result = manager.detectNeeded(sections, transcript);
        assertTrue(result.isPresent());
        assertEquals(CompactionStrategy.COMPACT, result.get());
    }

    @Test
    void detectNeededReturnsCompactWhenOverBudget() {
        // Over 100k tokens => > 400k chars
        var sections = List.of(new PromptSection("test", "a".repeat(450_000), false));
        var transcript = List.<TranscriptEntry>of();

        var result = manager.detectNeeded(sections, transcript);
        assertTrue(result.isPresent());
        assertEquals(CompactionStrategy.COMPACT, result.get());
    }

    @Test
    void compactionResultHasCorrectSizes() {
        var entries = generateEntries(25);
        var result = manager.compact(entries, CompactionStrategy.CLEAR);

        assertEquals(25, result.originalSize());
        assertEquals(10, result.compactedSize());
        assertEquals(10, result.compactedTranscript().size());
    }
}
