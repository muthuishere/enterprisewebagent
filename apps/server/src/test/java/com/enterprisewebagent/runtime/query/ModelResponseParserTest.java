package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.tools.ToolInvocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ModelResponseParserTest {

    @Test
    void extractToolCallsFromTextWithMarkers() {
        String response = "Let me read that file. [TOOL_CALL]{\"name\":\"file_read\", \"arguments\":{\"path\":\"/src/main.java\"}}[/TOOL_CALL] Done.";

        Optional<List<ToolInvocation>> result = ModelResponseParser.extractToolCalls(response);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
        assertEquals("file_read", result.get().getFirst().name());
        assertEquals("/src/main.java", result.get().getFirst().arguments().get("path"));
    }

    @Test
    void extractToolCallsReturnsEmptyForNoMarkers() {
        String response = "This is just a plain response with no tool calls.";

        Optional<List<ToolInvocation>> result = ModelResponseParser.extractToolCalls(response);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractToolCallsReturnsEmptyForNull() {
        assertTrue(ModelResponseParser.extractToolCalls(null).isEmpty());
        assertTrue(ModelResponseParser.extractToolCalls("").isEmpty());
    }

    @Test
    void extractMultipleToolCallsFromOneResponse() {
        String response = """
                First I'll read the file [TOOL_CALL]{"name":"file_read", "arguments":{"path":"/a.txt"}}[/TOOL_CALL] \
                then edit it [TOOL_CALL]{"name":"file_edit", "arguments":{"path":"/a.txt"}}[/TOOL_CALL] done.""";

        Optional<List<ToolInvocation>> result = ModelResponseParser.extractToolCalls(response);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().size());
        assertEquals("file_read", result.get().get(0).name());
        assertEquals("file_edit", result.get().get(1).name());
    }

    @Test
    void extractTextContentStripsMarkers() {
        String response = "Before [TOOL_CALL]{\"name\":\"shell\", \"arguments\":{\"command\":\"ls\"}}[/TOOL_CALL] After";

        String text = ModelResponseParser.extractTextContent(response);

        assertEquals("Before  After", text);
    }

    @Test
    void extractTextContentReturnsFullTextWhenNoMarkers() {
        String response = "Just plain text here.";

        String text = ModelResponseParser.extractTextContent(response);

        assertEquals("Just plain text here.", text);
    }

    @Test
    void extractTextContentHandlesNull() {
        assertEquals("", ModelResponseParser.extractTextContent(null));
    }

    @Test
    void extractToolCallsSkipsMalformedJson() {
        String response = "[TOOL_CALL]{not valid json}[/TOOL_CALL]";

        Optional<List<ToolInvocation>> result = ModelResponseParser.extractToolCalls(response);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractToolCallsHandlesNoArguments() {
        String response = "[TOOL_CALL]{\"name\":\"task_stop\", \"arguments\":{}}[/TOOL_CALL]";

        Optional<List<ToolInvocation>> result = ModelResponseParser.extractToolCalls(response);

        assertTrue(result.isPresent());
        assertEquals("task_stop", result.get().getFirst().name());
        assertTrue(result.get().getFirst().arguments().isEmpty());
    }
}
