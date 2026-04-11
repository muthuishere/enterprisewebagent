package com.enterprisewebagent.app.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void set_populatesMdcFields() {
        TraceContext.set("session-123", "turn-456");

        assertNotNull(TraceContext.traceId());
        assertEquals(8, TraceContext.traceId().length());
        assertEquals("session-123", TraceContext.sessionId());
        assertEquals("turn-456", TraceContext.turnId());
    }

    @Test
    void clear_removesMdcFields() {
        TraceContext.set("session-123", "turn-456");
        TraceContext.clear();

        assertNull(TraceContext.traceId());
        assertNull(TraceContext.sessionId());
        assertNull(TraceContext.turnId());
    }

    @Test
    void set_generatesUniqueTraceIds() {
        TraceContext.set("s1", "t1");
        String first = TraceContext.traceId();
        TraceContext.clear();

        TraceContext.set("s1", "t1");
        String second = TraceContext.traceId();

        assertNotEquals(first, second, "Each call should generate a unique traceId");
    }

    @Test
    void mdcValuesAreAccessibleDirectly() {
        TraceContext.set("sess-abc", "turn-def");

        assertEquals("sess-abc", MDC.get("sessionId"));
        assertEquals("turn-def", MDC.get("turnId"));
        assertNotNull(MDC.get("traceId"));
    }
}
