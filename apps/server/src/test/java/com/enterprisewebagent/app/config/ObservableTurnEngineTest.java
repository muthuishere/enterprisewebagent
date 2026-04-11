package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.query.TurnRequest;
import com.enterprisewebagent.runtime.query.TurnResult;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObservableTurnEngineTest {

    private SimpleMeterRegistry registry;
    private RuntimeMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new RuntimeMetrics(registry);
    }

    @Test
    void successfulTurn_recordsMetrics() {
        TurnEngine delegate = request -> new TurnResult(
                request.sessionId(), "Hello!", List.of(), true, List.of(), 10);
        ObservableTurnEngine engine = new ObservableTurnEngine(delegate, metrics);

        TurnResult result = engine.executeTurn(minimalRequest("Hi"));

        assertTrue(result.completed());
        assertEquals("Hello!", result.output());
        assertEquals(1.0, metrics.getTurnsTotal().count());
        assertEquals(1.0, metrics.getTurnsSucceeded().count());
        assertEquals(0.0, metrics.getTurnsFailed().count());
        assertEquals(1, metrics.getTurnDuration().count());
    }

    @Test
    void failedTurn_recordsFailureMetrics() {
        TurnEngine delegate = request -> new TurnResult(
                request.sessionId(), "error occurred", List.of(), false, List.of(), 0);
        ObservableTurnEngine engine = new ObservableTurnEngine(delegate, metrics);

        TurnResult result = engine.executeTurn(minimalRequest("Hi"));

        assertFalse(result.completed());
        assertEquals(1.0, metrics.getTurnsTotal().count());
        assertEquals(0.0, metrics.getTurnsSucceeded().count());
        assertEquals(1.0, metrics.getTurnsFailed().count());
    }

    @Test
    void exceptionInDelegate_recordsFailureAndRethrows() {
        TurnEngine delegate = request -> { throw new RuntimeException("boom"); };
        ObservableTurnEngine engine = new ObservableTurnEngine(delegate, metrics);

        assertThrows(RuntimeException.class, () -> engine.executeTurn(minimalRequest("Hi")));

        assertEquals(1.0, metrics.getTurnsTotal().count());
        assertEquals(0.0, metrics.getTurnsSucceeded().count());
        assertEquals(1.0, metrics.getTurnsFailed().count());
        assertEquals(1, metrics.getTurnDuration().count());
    }

    @Test
    void turnWithToolCalls_recordsToolCallMetrics() {
        var toolCalls = List.of(
                new ToolInvocation("file_read", Map.of("path", "/a.txt")),
                new ToolInvocation("shell", Map.of("command", "ls"))
        );
        TurnEngine delegate = request -> new TurnResult(
                request.sessionId(), "done", toolCalls, true, List.of(), 20);
        ObservableTurnEngine engine = new ObservableTurnEngine(delegate, metrics);

        engine.executeTurn(minimalRequest("Do stuff"));

        assertEquals(2.0, metrics.getToolCallsTotal().count());
        assertNotNull(registry.find("agent.tool_calls").tag("tool", "file_read").counter());
        assertNotNull(registry.find("agent.tool_calls").tag("tool", "shell").counter());
    }

    @Test
    void traceContextIsSetAndClearedDuringTurn() {
        TurnEngine delegate = request -> {
            assertNotNull(TraceContext.traceId(), "traceId should be set during turn");
            assertEquals(request.sessionId(), TraceContext.sessionId());
            return new TurnResult(request.sessionId(), "ok", List.of(), true, List.of(), 5);
        };
        ObservableTurnEngine engine = new ObservableTurnEngine(delegate, metrics);

        engine.executeTurn(minimalRequest("Hi"));

        assertNull(TraceContext.traceId(), "traceId should be cleared after turn");
    }

    private TurnRequest minimalRequest(String input) {
        return new TurnRequest(
                "test-session",
                input,
                List.of(new PromptSection("system", "You are a helpful assistant.", false)),
                List.of()
        );
    }
}
