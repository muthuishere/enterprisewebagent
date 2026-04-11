package com.enterprisewebagent.runtime.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEventPublisherTest {

    private InMemoryEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new InMemoryEventPublisher();
    }

    @Test
    void publishDispatchesToRegisteredListener() {
        var received = new ArrayList<RuntimeEvent>();
        publisher.addListener(received::add);

        var event = new TurnStartedEvent("s1", Instant.now());
        publisher.publish(event);

        assertEquals(1, received.size());
        assertSame(event, received.getFirst());
    }

    @Test
    void publishDispatchesToMultipleListeners() {
        var received1 = new ArrayList<RuntimeEvent>();
        var received2 = new ArrayList<RuntimeEvent>();
        publisher.addListener(received1::add);
        publisher.addListener(received2::add);

        var event = new TurnStartedEvent("s1", Instant.now());
        publisher.publish(event);

        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
    }

    @Test
    void removeListenerStopsReceiving() {
        var received = new ArrayList<RuntimeEvent>();
        RuntimeEventListener listener = received::add;
        publisher.addListener(listener);
        publisher.removeListener(listener);

        publisher.publish(new TurnStartedEvent("s1", Instant.now()));

        assertTrue(received.isEmpty());
    }

    @Test
    void publishWithNoListenersDoesNotThrow() {
        assertDoesNotThrow(() ->
                publisher.publish(new TurnStartedEvent("s1", Instant.now())));
    }
}
