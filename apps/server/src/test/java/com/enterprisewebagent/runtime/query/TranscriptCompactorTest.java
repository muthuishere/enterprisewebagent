package com.enterprisewebagent.runtime.query;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TranscriptCompactorTest {

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
    void clearKeepsOnlyLastNEntries() {
        var entries = generateEntries(30);
        var result = TranscriptCompactor.compact(entries, CompactionStrategy.CLEAR);
        assertEquals(10, result.size());
        assertEquals("Message 20", result.getFirst().content());
        assertEquals("Message 29", result.getLast().content());
    }

    @Test
    void compactCreatesSummaryPlusRecentEntries() {
        var entries = generateEntries(40);
        var result = TranscriptCompactor.compact(entries, CompactionStrategy.COMPACT);
        // 1 summary + 20 recent = 21
        assertEquals(21, result.size());
        assertEquals("system", result.getFirst().role());
        assertTrue(result.getFirst().content().startsWith("[Compacted conversation summary:"));
    }

    @Test
    void truncateKeepsOnlyLast2() {
        var entries = generateEntries(50);
        var result = TranscriptCompactor.compact(entries, CompactionStrategy.TRUNCATE);
        assertEquals(2, result.size());
        assertEquals("Message 48", result.get(0).content());
        assertEquals("Message 49", result.get(1).content());
    }

    @Test
    void emptyTranscriptReturnsEmpty() {
        var result = TranscriptCompactor.compact(List.of(), CompactionStrategy.CLEAR);
        assertTrue(result.isEmpty());

        result = TranscriptCompactor.compact(List.of(), CompactionStrategy.COMPACT);
        assertTrue(result.isEmpty());

        result = TranscriptCompactor.compact(List.of(), CompactionStrategy.TRUNCATE);
        assertTrue(result.isEmpty());
    }

    @Test
    void smallTranscriptBelowThresholdReturnsUnchanged() {
        var entries = generateEntries(5);
        var resultClear = TranscriptCompactor.compact(entries, CompactionStrategy.CLEAR);
        assertEquals(5, resultClear.size());

        var resultCompact = TranscriptCompactor.compact(entries, CompactionStrategy.COMPACT);
        assertEquals(5, resultCompact.size());
    }

    @Test
    void summaryIncludesMessageCounts() {
        var entries = List.of(
                entry("user", "Hello"),
                entry("assistant", "Hi there"),
                entry("user", "Do something"),
                entry("tool_call", "tool: args"),
                entry("tool_result", "tool: result"),
                entry("assistant", "Done")
        );
        String summary = TranscriptCompactor.summarizeEntries(entries);
        assertTrue(summary.contains("2 user messages"));
        assertTrue(summary.contains("2 assistant messages"));
        assertTrue(summary.contains("2 tool interactions"));
        assertTrue(summary.contains("First topic: Hello"));
    }

    @Test
    void compactWithExactlyThresholdEntriesReturnsUnchanged() {
        var entries = generateEntries(20);
        var result = TranscriptCompactor.compact(entries, CompactionStrategy.COMPACT);
        // No old entries to summarize, so just the 20 recent
        assertEquals(20, result.size());
    }
}
