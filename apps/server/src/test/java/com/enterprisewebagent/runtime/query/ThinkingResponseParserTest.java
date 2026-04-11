package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.query.ThinkingResponseParser.ThinkingResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThinkingResponseParserTest {

    @Test
    void parse_noThinking_returnsFullTextAsAnswer() {
        ThinkingResult result = ThinkingResponseParser.parse("Hello, how can I help?");
        assertNull(result.thinkingContent());
        assertEquals("Hello, how can I help?", result.answer());
        assertFalse(result.hasThinking());
    }

    @Test
    void parse_withThinking_extractsBoth() {
        String response = "<thinking>Let me consider the options...</thinking>Here is my answer.";
        ThinkingResult result = ThinkingResponseParser.parse(response);

        assertTrue(result.hasThinking());
        assertEquals("Let me consider the options...", result.thinkingContent());
        assertEquals("Here is my answer.", result.answer());
    }

    @Test
    void parse_multilineThinking() {
        String response = "<thinking>\nStep 1: analyze\nStep 2: plan\nStep 3: execute\n</thinking>\nFinal answer here.";
        ThinkingResult result = ThinkingResponseParser.parse(response);

        assertTrue(result.hasThinking());
        assertTrue(result.thinkingContent().contains("Step 1: analyze"));
        assertTrue(result.thinkingContent().contains("Step 3: execute"));
        assertEquals("Final answer here.", result.answer());
    }

    @Test
    void parse_multipleThinkingBlocks() {
        String response = "<thinking>First thought</thinking>Middle text<thinking>Second thought</thinking>Final text";
        ThinkingResult result = ThinkingResponseParser.parse(response);

        assertTrue(result.hasThinking());
        assertTrue(result.thinkingContent().contains("First thought"));
        assertTrue(result.thinkingContent().contains("Second thought"));
        assertEquals("Middle textFinal text", result.answer());
    }

    @Test
    void parse_emptyString() {
        ThinkingResult result = ThinkingResponseParser.parse("");
        assertNull(result.thinkingContent());
        assertEquals("", result.answer());
    }

    @Test
    void parse_null() {
        ThinkingResult result = ThinkingResponseParser.parse(null);
        assertNull(result.thinkingContent());
        assertEquals("", result.answer());
    }

    @Test
    void parse_thinkingOnly_emptyAnswer() {
        String response = "<thinking>My thought process</thinking>";
        ThinkingResult result = ThinkingResponseParser.parse(response);
        assertTrue(result.hasThinking());
        assertEquals("My thought process", result.thinkingContent());
        assertEquals("", result.answer());
    }

    @Test
    void containsThinking_true() {
        assertTrue(ThinkingResponseParser.containsThinking("<thinking>content</thinking>"));
    }

    @Test
    void containsThinking_false() {
        assertFalse(ThinkingResponseParser.containsThinking("No thinking here"));
    }

    @Test
    void containsThinking_null() {
        assertFalse(ThinkingResponseParser.containsThinking(null));
    }
}
