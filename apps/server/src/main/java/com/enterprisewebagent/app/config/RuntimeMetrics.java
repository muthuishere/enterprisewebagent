package com.enterprisewebagent.app.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RuntimeMetrics {

    private final MeterRegistry registry;
    private final Counter turnsTotal;
    private final Counter turnsSucceeded;
    private final Counter turnsFailed;
    private final Counter toolCallsTotal;
    private final Timer turnDuration;
    private final AtomicInteger activeSessions = new AtomicInteger(0);
    private final AtomicInteger activeWebSockets = new AtomicInteger(0);

    public RuntimeMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.turnsTotal = Counter.builder("agent.turns.total")
                .description("Total number of turns executed")
                .register(registry);
        this.turnsSucceeded = Counter.builder("agent.turns.succeeded")
                .description("Number of successfully completed turns")
                .register(registry);
        this.turnsFailed = Counter.builder("agent.turns.failed")
                .description("Number of failed turns")
                .register(registry);
        this.toolCallsTotal = Counter.builder("agent.tool_calls.total")
                .description("Total number of tool calls")
                .register(registry);
        this.turnDuration = Timer.builder("agent.turns.duration")
                .description("Duration of turn execution")
                .register(registry);

        Gauge.builder("agent.sessions.active", activeSessions, AtomicInteger::get)
                .description("Number of active sessions")
                .register(registry);
        Gauge.builder("agent.websockets.active", activeWebSockets, AtomicInteger::get)
                .description("Number of active WebSocket connections")
                .register(registry);
    }

    public void recordTurnStarted() {
        turnsTotal.increment();
    }

    public void recordTurnSucceeded() {
        turnsSucceeded.increment();
    }

    public void recordTurnFailed() {
        turnsFailed.increment();
    }

    public void recordToolCall(String toolName) {
        toolCallsTotal.increment();
        Counter.builder("agent.tool_calls")
                .tag("tool", toolName)
                .register(registry)
                .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(turnDuration);
    }

    public void sessionOpened() {
        activeSessions.incrementAndGet();
    }

    public void sessionClosed() {
        activeSessions.decrementAndGet();
    }

    public void webSocketConnected() {
        activeWebSockets.incrementAndGet();
    }

    public void webSocketDisconnected() {
        activeWebSockets.decrementAndGet();
    }

    // Visible for testing
    Counter getTurnsTotal() { return turnsTotal; }
    Counter getTurnsSucceeded() { return turnsSucceeded; }
    Counter getTurnsFailed() { return turnsFailed; }
    Counter getToolCallsTotal() { return toolCallsTotal; }
    Timer getTurnDuration() { return turnDuration; }
    AtomicInteger getActiveSessions() { return activeSessions; }
    AtomicInteger getActiveWebSockets() { return activeWebSockets; }
}
