package com.enterprisewebagent.runtime.memory;

import com.enterprisewebagent.runtime.query.TurnResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySessionMemoryTest {

    private InMemoryMemoryStore store;
    private InMemorySessionMemory sessionMemory;

    @BeforeEach
    void setUp() {
        store = new InMemoryMemoryStore();
        sessionMemory = new InMemorySessionMemory("session-1", store);
    }

    @Test
    void emptyMemoryReturnsEmptyString() {
        assertEquals("", sessionMemory.getMemoryPrompt());
    }

    @Test
    void afterTurnCompleteMemoryPromptIncludesTurnInfo() {
        var result = new TurnResult("session-1", "The quick brown fox jumped", List.of(), true);
        sessionMemory.onTurnComplete(result);

        var prompt = sessionMemory.getMemoryPrompt();
        assertTrue(prompt.startsWith("## Session Memory\n\n"));
        assertTrue(prompt.contains("The quick brown fox jumped"));
    }

    @Test
    void blankOutputIsNotStored() {
        var result = new TurnResult("session-1", "  ", List.of(), true);
        sessionMemory.onTurnComplete(result);

        assertEquals("", sessionMemory.getMemoryPrompt());
    }

    @Test
    void longOutputIsTruncatedTo200Chars() {
        var longOutput = "x".repeat(500);
        var result = new TurnResult("session-1", longOutput, List.of(), true);
        sessionMemory.onTurnComplete(result);

        var prompt = sessionMemory.getMemoryPrompt();
        // The stored content should be at most 200 chars
        assertTrue(prompt.contains("x".repeat(200)));
        assertFalse(prompt.contains("x".repeat(201)));
    }
}
