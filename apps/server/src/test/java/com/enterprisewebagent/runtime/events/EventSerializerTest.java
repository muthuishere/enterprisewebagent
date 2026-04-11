package com.enterprisewebagent.runtime.events;

import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventSerializerTest {

    @Test
    void turnStartedEventSerialization() {
        var ts = Instant.parse("2025-01-15T10:30:00Z");
        var event = new TurnStartedEvent("session-1", ts);
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"turn_started\""));
        assertTrue(json.contains("\"sessionId\":\"session-1\""));
        assertTrue(json.contains("\"timestamp\":\"2025-01-15T10:30:00Z\""));
    }

    @Test
    void tokenDeltaEventSerialization() {
        var event = new TokenDeltaEvent("s1", "hello world");
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"token_delta\""));
        assertTrue(json.contains("\"text\":\"hello world\""));
    }

    @Test
    void toolRequestedEventSerializationIncludesArguments() {
        var args = Map.<String, Object>of("path", "/tmp/test", "recursive", true);
        var event = new ToolRequestedEvent("s1", "readFile", args);
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"tool_requested\""));
        assertTrue(json.contains("\"toolName\":\"readFile\""));
        assertTrue(json.contains("\"path\":\"/tmp/test\""));
        assertTrue(json.contains("\"recursive\":true"));
    }

    @Test
    void toolCompletedEventSerialization() {
        var result = new ToolResult("readFile", "file contents", true);
        var event = new ToolCompletedEvent("s1", "readFile", result);
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"tool_completed\""));
        assertTrue(json.contains("\"toolName\":\"readFile\""));
        assertTrue(json.contains("\"success\":true"));
    }

    @Test
    void taskStateChangedEventSerialization() {
        var event = new TaskStateChangedEvent("s1", "task-1", "RUNNING");
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"task_state_changed\""));
        assertTrue(json.contains("\"taskId\":\"task-1\""));
        assertTrue(json.contains("\"newState\":\"RUNNING\""));
    }

    @Test
    void workerStateChangedEventSerialization() {
        var event = new WorkerStateChangedEvent("s1", "worker-1", "IDLE");
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"worker_state_changed\""));
        assertTrue(json.contains("\"workerId\":\"worker-1\""));
        assertTrue(json.contains("\"newState\":\"IDLE\""));
    }

    @Test
    void turnCompletedEventSerialization() {
        var ts = Instant.parse("2025-01-15T10:31:00Z");
        var event = new TurnCompletedEvent("s1", "done", ts);
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"turn_completed\""));
        assertTrue(json.contains("\"output\":\"done\""));
        assertTrue(json.contains("\"timestamp\":\"2025-01-15T10:31:00Z\""));
    }

    @Test
    void turnFailedEventSerialization() {
        var ts = Instant.parse("2025-01-15T10:32:00Z");
        var event = new TurnFailedEvent("s1", "something broke", ts);
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"turn_failed\""));
        assertTrue(json.contains("\"error\":\"something broke\""));
    }

    @Test
    void askUserRequestedEventSerialization() {
        var event = new AskUserRequestedEvent("s1", "Pick a color", java.util.List.of("red", "blue"));
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"ask_user_requested\""));
        assertTrue(json.contains("\"sessionId\":\"s1\""));
        assertTrue(json.contains("\"question\":\"Pick a color\""));
        assertTrue(json.contains("\"choices\":[\"red\",\"blue\"]"));
    }

    @Test
    void askUserRequestedEventSerializationEmptyChoices() {
        var event = new AskUserRequestedEvent("s2", "What now?", java.util.List.of());
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\"type\":\"ask_user_requested\""));
        assertTrue(json.contains("\"question\":\"What now?\""));
        assertTrue(json.contains("\"choices\":[]"));
    }

    @Test
    void specialCharactersAreEscaped() {
        var event = new TokenDeltaEvent("s1", "line1\nline2\t\"quoted\"");
        var json = EventSerializer.toJson(event);

        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\\"quoted\\\""));
    }
}
