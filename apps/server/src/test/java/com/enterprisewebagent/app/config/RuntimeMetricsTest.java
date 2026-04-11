package com.enterprisewebagent.app.config;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeMetricsTest {

    private SimpleMeterRegistry registry;
    private RuntimeMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new RuntimeMetrics(registry);
    }

    @Test
    void recordTurnStarted_incrementsCounter() {
        metrics.recordTurnStarted();
        metrics.recordTurnStarted();

        assertEquals(2.0, metrics.getTurnsTotal().count());
    }

    @Test
    void recordTurnSucceeded_incrementsCounter() {
        metrics.recordTurnSucceeded();

        assertEquals(1.0, metrics.getTurnsSucceeded().count());
    }

    @Test
    void recordTurnFailed_incrementsCounter() {
        metrics.recordTurnFailed();
        metrics.recordTurnFailed();
        metrics.recordTurnFailed();

        assertEquals(3.0, metrics.getTurnsFailed().count());
    }

    @Test
    void recordToolCall_incrementsTotalAndPerToolCounter() {
        metrics.recordToolCall("file_read");
        metrics.recordToolCall("shell");
        metrics.recordToolCall("file_read");

        assertEquals(3.0, metrics.getToolCallsTotal().count());

        var fileReadCounter = registry.find("agent.tool_calls").tag("tool", "file_read").counter();
        assertNotNull(fileReadCounter);
        assertEquals(2.0, fileReadCounter.count());

        var shellCounter = registry.find("agent.tool_calls").tag("tool", "shell").counter();
        assertNotNull(shellCounter);
        assertEquals(1.0, shellCounter.count());
    }

    @Test
    void timerRecordsDuration() throws InterruptedException {
        Timer.Sample sample = metrics.startTimer();
        Thread.sleep(10);
        metrics.stopTimer(sample);

        assertEquals(1, metrics.getTurnDuration().count());
        assertTrue(metrics.getTurnDuration().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 5);
    }

    @Test
    void sessionGaugeTracksOpenAndClose() {
        metrics.sessionOpened();
        metrics.sessionOpened();
        assertEquals(2, metrics.getActiveSessions().get());

        metrics.sessionClosed();
        assertEquals(1, metrics.getActiveSessions().get());
    }

    @Test
    void webSocketGaugeTracksConnectAndDisconnect() {
        metrics.webSocketConnected();
        assertEquals(1, metrics.getActiveWebSockets().get());

        metrics.webSocketDisconnected();
        assertEquals(0, metrics.getActiveWebSockets().get());
    }

    @Test
    void metricsRegisteredInMeterRegistry() {
        assertNotNull(registry.find("agent.turns.total").counter());
        assertNotNull(registry.find("agent.turns.succeeded").counter());
        assertNotNull(registry.find("agent.turns.failed").counter());
        assertNotNull(registry.find("agent.tool_calls.total").counter());
        assertNotNull(registry.find("agent.turns.duration").timer());
        assertNotNull(registry.find("agent.sessions.active").gauge());
        assertNotNull(registry.find("agent.websockets.active").gauge());
    }
}
