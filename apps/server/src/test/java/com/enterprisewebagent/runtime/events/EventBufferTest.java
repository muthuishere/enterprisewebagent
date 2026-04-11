package com.enterprisewebagent.runtime.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EventBufferTest {

    private EventBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new EventBuffer();
    }

    @Test
    void eventsAreBuffered() {
        var event = new TurnStartedEvent("s1", Instant.now());
        buffer.onEvent(event);

        var events = buffer.getEvents();
        assertEquals(1, events.size());
        assertSame(event, events.getFirst());
    }

    @Test
    void sessionIdFilteringWorks() {
        buffer.onEvent(new TurnStartedEvent("s1", Instant.now()));
        buffer.onEvent(new TurnStartedEvent("s2", Instant.now()));
        buffer.onEvent(new TokenDeltaEvent("s1", "hello"));

        var s1Events = buffer.getEvents("s1");
        assertEquals(2, s1Events.size());

        var s2Events = buffer.getEvents("s2");
        assertEquals(1, s2Events.size());
    }

    @Test
    void capacityLimitEvictsOldestEvents() {
        var smallBuffer = new EventBuffer(3);

        smallBuffer.onEvent(new TurnStartedEvent("s1", Instant.now()));
        smallBuffer.onEvent(new TokenDeltaEvent("s1", "a"));
        smallBuffer.onEvent(new TokenDeltaEvent("s1", "b"));
        smallBuffer.onEvent(new TokenDeltaEvent("s1", "c"));

        var events = smallBuffer.getEvents();
        assertEquals(3, events.size());
        // oldest (TurnStartedEvent) should have been evicted
        assertInstanceOf(TokenDeltaEvent.class, events.getFirst());
    }

    @Test
    void clearEmptiesBuffer() {
        buffer.onEvent(new TurnStartedEvent("s1", Instant.now()));
        buffer.onEvent(new TokenDeltaEvent("s1", "hello"));

        buffer.clear();

        assertTrue(buffer.getEvents().isEmpty());
    }
}
