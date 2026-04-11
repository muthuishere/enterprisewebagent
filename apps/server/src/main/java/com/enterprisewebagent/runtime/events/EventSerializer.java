package com.enterprisewebagent.runtime.events;

import java.util.Map;

public final class EventSerializer {

    private EventSerializer() {}

    public static String toJson(RuntimeEvent event) {
        return switch (event) {
            case TurnStartedEvent e -> buildJson("turn_started",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"timestamp\":" + quote(e.timestamp().toString()));

            case TokenDeltaEvent e -> buildJson("token_delta",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"text\":" + quote(escape(e.text())));

            case ToolRequestedEvent e -> buildJson("tool_requested",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"toolName\":" + quote(e.toolName()),
                    "\"arguments\":" + mapToJson(e.arguments()));

            case ToolCompletedEvent e -> buildJson("tool_completed",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"toolName\":" + quote(e.toolName()),
                    "\"result\":{" +
                            "\"name\":" + quote(e.result().name()) + "," +
                            "\"output\":" + quote(escape(e.result().output())) + "," +
                            "\"success\":" + e.result().success() + "}");

            case TaskStateChangedEvent e -> buildJson("task_state_changed",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"taskId\":" + quote(e.taskId()),
                    "\"newState\":" + quote(e.newState()));

            case WorkerStateChangedEvent e -> buildJson("worker_state_changed",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"workerId\":" + quote(e.workerId()),
                    "\"newState\":" + quote(e.newState()));

            case TurnCompletedEvent e -> buildJson("turn_completed",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"output\":" + quote(escape(e.output())),
                    "\"timestamp\":" + quote(e.timestamp().toString()));

            case TurnFailedEvent e -> buildJson("turn_failed",
                    "\"sessionId\":" + quote(e.sessionId()),
                    "\"error\":" + quote(escape(e.error())),
                    "\"timestamp\":" + quote(e.timestamp().toString()));
        };
    }

    private static String buildJson(String type, String... fields) {
        var sb = new StringBuilder();
        sb.append("{\"type\":").append(quote(type));
        for (String field : fields) {
            sb.append(',').append(field);
        }
        sb.append('}');
        return sb.toString();
    }

    private static String quote(String value) {
        if (value == null) return "null";
        return "\"" + value + "\"";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        var sb = new StringBuilder("{");
        var first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(quote(entry.getKey())).append(':');
            sb.append(valueToJson(entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return quote(escape(s));
        if (value instanceof Number) return value.toString();
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> m) return mapToJson((Map<String, Object>) m);
        return quote(escape(value.toString()));
    }
}
