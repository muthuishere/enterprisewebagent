package com.enterprisewebagent.runtime.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TranscriptTest {

    private Transcript transcript;

    @BeforeEach
    void setUp() {
        transcript = new Transcript();
    }

    @Test
    void addUserMessageCreatesEntry() {
        transcript.addUserMessage("Hello");

        assertEquals(1, transcript.size());
        var entry = transcript.entries().getFirst();
        assertEquals("user", entry.role());
        assertEquals("Hello", entry.content());
        assertNotNull(entry.timestamp());
    }

    @Test
    void addAssistantMessageCreatesEntry() {
        transcript.addAssistantMessage("Hi there");

        assertEquals(1, transcript.size());
        var entry = transcript.entries().getFirst();
        assertEquals("assistant", entry.role());
        assertEquals("Hi there", entry.content());
    }

    @Test
    void addToolCallCreatesEntry() {
        transcript.addToolCall("file_read", "{\"path\":\"/tmp\"}");

        assertEquals(1, transcript.size());
        var entry = transcript.entries().getFirst();
        assertEquals("tool_call", entry.role());
        assertTrue(entry.content().contains("file_read"));
        assertTrue(entry.content().contains("{\"path\":\"/tmp\"}"));
    }

    @Test
    void addToolResultCreatesEntry() {
        transcript.addToolResult("file_read", "contents of file");

        assertEquals(1, transcript.size());
        var entry = transcript.entries().getFirst();
        assertEquals("tool_result", entry.role());
        assertTrue(entry.content().contains("file_read"));
        assertTrue(entry.content().contains("contents of file"));
    }

    @Test
    void recentEntriesReturnsLastN() {
        transcript.addUserMessage("first");
        transcript.addAssistantMessage("second");
        transcript.addUserMessage("third");
        transcript.addAssistantMessage("fourth");

        var recent = transcript.recentEntries(2);
        assertEquals(2, recent.size());
        assertEquals("third", recent.get(0).content());
        assertEquals("fourth", recent.get(1).content());
    }

    @Test
    void recentEntriesReturnsAllWhenMaxExceedsSize() {
        transcript.addUserMessage("only");

        var recent = transcript.recentEntries(5);
        assertEquals(1, recent.size());
        assertEquals("only", recent.getFirst().content());
    }

    @Test
    void entriesReturnsUnmodifiableList() {
        transcript.addUserMessage("msg");

        var entries = transcript.entries();
        assertThrows(UnsupportedOperationException.class, () -> entries.add(null));
    }

    @Test
    void sizeReflectsAllAdditions() {
        assertEquals(0, transcript.size());
        transcript.addUserMessage("a");
        transcript.addAssistantMessage("b");
        transcript.addToolCall("t", "args");
        transcript.addToolResult("t", "result");
        assertEquals(4, transcript.size());
    }
}
