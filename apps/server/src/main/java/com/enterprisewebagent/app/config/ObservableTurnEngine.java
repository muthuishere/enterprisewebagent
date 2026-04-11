package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.query.TurnRequest;
import com.enterprisewebagent.runtime.query.TurnResult;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservableTurnEngine implements TurnEngine {

    private static final Logger log = LoggerFactory.getLogger(ObservableTurnEngine.class);

    private final TurnEngine delegate;
    private final RuntimeMetrics metrics;

    public ObservableTurnEngine(TurnEngine delegate, RuntimeMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public TurnResult executeTurn(TurnRequest request) {
        String sessionId = request.sessionId();
        TraceContext.set(sessionId, java.util.UUID.randomUUID().toString().substring(0, 8));

        metrics.recordTurnStarted();
        Timer.Sample sample = metrics.startTimer();

        log.info("Turn started sessionId={} input_length={}", sessionId, request.input().length());

        try {
            TurnResult result = delegate.executeTurn(request);

            if (result.completed()) {
                metrics.recordTurnSucceeded();
                log.info("Turn completed sessionId={} toolCalls={} output_length={}",
                        sessionId, result.toolCalls().size(), result.output().length());
            } else {
                metrics.recordTurnFailed();
                log.warn("Turn failed sessionId={} error={}", sessionId, result.output());
            }

            for (var toolCall : result.toolCalls()) {
                metrics.recordToolCall(toolCall.name());
            }

            return result;
        } catch (Exception e) {
            metrics.recordTurnFailed();
            log.error("Turn exception sessionId={} error={}", sessionId, e.getMessage(), e);
            throw e;
        } finally {
            metrics.stopTimer(sample);
            TraceContext.clear();
        }
    }

    // Visible for testing
    TurnEngine getDelegate() {
        return delegate;
    }
}
