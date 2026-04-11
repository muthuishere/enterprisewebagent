package com.enterprisewebagent.app.config;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceContext {

    private TraceContext() {}

    public static void set(String sessionId, String turnId) {
        MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        MDC.put("sessionId", sessionId);
        MDC.put("turnId", turnId);
    }

    public static void clear() {
        MDC.clear();
    }

    public static String traceId() {
        return MDC.get("traceId");
    }

    public static String sessionId() {
        return MDC.get("sessionId");
    }

    public static String turnId() {
        return MDC.get("turnId");
    }
}
