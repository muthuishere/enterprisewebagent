package com.enterprisewebagent.runtime.permissions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DenialTrackerTest {

    private DenialTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new DenialTracker();
    }

    @Test
    void recordAndRetrieveDenial() {
        Instant now = Instant.now();
        tracker.recordDenial("shell", "dangerous command", now);

        List<Denial> recent = tracker.getRecent(10);
        assertEquals(1, recent.size());
        assertEquals("shell", recent.get(0).toolName());
        assertEquals("dangerous command", recent.get(0).reason());
        assertEquals(now, recent.get(0).timestamp());
    }

    @Test
    void getRecentReturnsLastN() {
        for (int i = 0; i < 20; i++) {
            tracker.recordDenial("tool-" + i, "reason-" + i, Instant.now());
        }

        List<Denial> recent = tracker.getRecent(5);
        assertEquals(5, recent.size());
        assertEquals("tool-15", recent.get(0).toolName());
        assertEquals("tool-19", recent.get(4).toolName());
    }

    @Test
    void getRecentWithMoreThanAvailable() {
        tracker.recordDenial("shell", "test", Instant.now());

        List<Denial> recent = tracker.getRecent(100);
        assertEquals(1, recent.size());
    }

    @Test
    void getDenialCounts() {
        tracker.recordDenial("shell", "reason1", Instant.now());
        tracker.recordDenial("shell", "reason2", Instant.now());
        tracker.recordDenial("file_edit", "reason3", Instant.now());

        Map<String, Long> counts = tracker.getDenialCounts();
        assertEquals(2L, counts.get("shell"));
        assertEquals(1L, counts.get("file_edit"));
    }

    @Test
    void emptyTrackerReturnsEmpty() {
        assertTrue(tracker.getRecent(10).isEmpty());
        assertTrue(tracker.getDenialCounts().isEmpty());
    }

    @Test
    void sizeReturnsCorrectCount() {
        assertEquals(0, tracker.size());
        tracker.recordDenial("shell", "test", Instant.now());
        assertEquals(1, tracker.size());
        tracker.recordDenial("shell", "test2", Instant.now());
        assertEquals(2, tracker.size());
    }
}
