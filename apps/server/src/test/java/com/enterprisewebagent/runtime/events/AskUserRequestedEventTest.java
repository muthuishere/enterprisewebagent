package com.enterprisewebagent.runtime.events;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AskUserRequestedEventTest {

    @Test
    void eventCreationWithChoices() {
        var event = new AskUserRequestedEvent("session-1", "Pick a color", List.of("red", "blue"));

        assertEquals("session-1", event.sessionId());
        assertEquals("Pick a color", event.question());
        assertEquals(List.of("red", "blue"), event.choices());
    }

    @Test
    void eventCreationWithEmptyChoices() {
        var event = new AskUserRequestedEvent("s2", "What is your name?", List.of());

        assertEquals("s2", event.sessionId());
        assertEquals("What is your name?", event.question());
        assertTrue(event.choices().isEmpty());
    }

    @Test
    void eventImplementsRuntimeEvent() {
        RuntimeEvent event = new AskUserRequestedEvent("s1", "question", List.of());
        assertInstanceOf(RuntimeEvent.class, event);
    }
}
