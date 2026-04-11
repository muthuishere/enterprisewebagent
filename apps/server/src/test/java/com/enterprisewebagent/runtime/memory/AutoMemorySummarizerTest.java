package com.enterprisewebagent.runtime.memory;

import com.enterprisewebagent.runtime.query.TranscriptEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoMemorySummarizerTest {

    private final AutoMemorySummarizer summarizer = new AutoMemorySummarizer();

    @Test
    void summarize_emptyListReturnsEmpty() {
        assertEquals("", summarizer.summarize(List.of()));
    }

    @Test
    void summarize_nullListReturnsEmpty() {
        assertEquals("", summarizer.summarize(null));
    }

    @Test
    void summarize_extractsDecisions() {
        var entries = List.of(
                new TranscriptEntry("assistant", "We decided to use PostgreSQL for the database", Instant.now()),
                new TranscriptEntry("user", "Sounds good", Instant.now())
        );

        String result = summarizer.summarize(entries);
        assertTrue(result.contains("### Decisions"));
        assertTrue(result.contains("decided"));
    }

    @Test
    void summarize_extractsErrors() {
        var entries = List.of(
                new TranscriptEntry("assistant", "I encountered an error: NullPointerException in line 42", Instant.now())
        );

        String result = summarizer.summarize(entries);
        assertTrue(result.contains("### Errors Encountered"));
        assertTrue(result.contains("error"));
    }

    @Test
    void summarize_extractsFileChanges() {
        var entries = List.of(
                new TranscriptEntry("assistant", "I created src/main/App.java with the main class", Instant.now()),
                new TranscriptEntry("assistant", "I modified build.gradle to add dependency", Instant.now())
        );

        String result = summarizer.summarize(entries);
        assertTrue(result.contains("### File Changes"));
    }

    @Test
    void summarize_handlesBlankContent() {
        var entries = List.of(
                new TranscriptEntry("user", "  ", Instant.now()),
                new TranscriptEntry("assistant", "", Instant.now())
        );

        String result = summarizer.summarize(entries);
        assertEquals("", result);
    }
}
