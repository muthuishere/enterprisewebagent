package com.enterprisewebagent.runtime.cost;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SessionCostTrackerTest {

    private SessionCostTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new SessionCostTracker();
    }

    @Test
    void recordAndGetLastTurnCost() {
        TurnCost cost = new TurnCost("gpt-4o", 1000, 500, 0, 0.0075, Duration.ofSeconds(2));
        tracker.recordTurnCost("session-1", cost);

        TurnCost last = tracker.getLastTurnCost("session-1");
        assertNotNull(last);
        assertEquals("gpt-4o", last.model());
        assertEquals(1000, last.inputTokens());
        assertEquals(0.0075, last.totalUsd(), 0.0001);
    }

    @Test
    void getLastTurnCostReturnsNull() {
        assertNull(tracker.getLastTurnCost("nonexistent"));
    }

    @Test
    void getSessionSummary() {
        tracker.recordTurnCost("s1", new TurnCost("gpt-4o", 1000, 500, 0, 0.01, Duration.ofSeconds(1)));
        tracker.recordTurnCost("s1", new TurnCost("gpt-4o", 2000, 1000, 0, 0.02, Duration.ofSeconds(2)));

        SessionCostSummary summary = tracker.getSessionSummary("s1");
        assertEquals("s1", summary.sessionId());
        assertEquals(2, summary.totalTurns());
        assertEquals(3000, summary.totalInputTokens());
        assertEquals(1500, summary.totalOutputTokens());
        assertEquals(0.03, summary.totalUsd(), 0.001);
        assertEquals(2, summary.turns().size());
    }

    @Test
    void getSessionSummaryForUnknownSession() {
        SessionCostSummary summary = tracker.getSessionSummary("unknown");
        assertEquals(0, summary.totalTurns());
        assertEquals(0.0, summary.totalUsd());
    }

    @Test
    void getTotalCostAcrossSessions() {
        tracker.recordTurnCost("s1", new TurnCost("gpt-4o", 1000, 500, 0, 0.01, Duration.ofSeconds(1)));
        tracker.recordTurnCost("s2", new TurnCost("gpt-4o", 2000, 1000, 0, 0.02, Duration.ofSeconds(2)));

        assertEquals(0.03, tracker.getTotalCost(), 0.001);
    }

    @Test
    void totalCostEmptyTracker() {
        assertEquals(0.0, tracker.getTotalCost());
    }

    @Test
    void multipleSessionsTrackedIndependently() {
        tracker.recordTurnCost("s1", new TurnCost("gpt-4o", 100, 50, 0, 0.001, Duration.ofSeconds(1)));
        tracker.recordTurnCost("s2", new TurnCost("claude-sonnet-4-20250514", 200, 100, 0, 0.002, Duration.ofSeconds(1)));

        SessionCostSummary s1 = tracker.getSessionSummary("s1");
        SessionCostSummary s2 = tracker.getSessionSummary("s2");
        assertEquals(1, s1.totalTurns());
        assertEquals(1, s2.totalTurns());
        assertEquals(100, s1.totalInputTokens());
        assertEquals(200, s2.totalInputTokens());
    }

    @Test
    void lastTurnCostReturnsLatest() {
        tracker.recordTurnCost("s1", new TurnCost("gpt-4o", 100, 50, 0, 0.001, Duration.ofSeconds(1)));
        tracker.recordTurnCost("s1", new TurnCost("gpt-4o-mini", 200, 100, 0, 0.002, Duration.ofSeconds(2)));

        TurnCost last = tracker.getLastTurnCost("s1");
        assertEquals("gpt-4o-mini", last.model());
    }
}
