package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.tools.ToolInvocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses model response text to detect tool call requests.
 * Uses a simple marker convention: [TOOL_CALL]{...}[/TOOL_CALL].
 * This is a placeholder parser — the real one will be driven by
 * the model provider's native tool-calling format.
 */
public final class ModelResponseParser {

    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("\\[TOOL_CALL](\\{.*?})\\[/TOOL_CALL]", Pattern.DOTALL);

    private ModelResponseParser() {}

    public static Optional<List<ToolInvocation>> extractToolCalls(String responseText) {
        if (responseText == null || responseText.isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = TOOL_CALL_PATTERN.matcher(responseText);
        List<ToolInvocation> invocations = new ArrayList<>();

        while (matcher.find()) {
            String json = matcher.group(1);
            parseToolCallJson(json).ifPresent(invocations::add);
        }

        return invocations.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(invocations));
    }

    public static String extractTextContent(String responseText) {
        if (responseText == null) {
            return "";
        }
        return TOOL_CALL_PATTERN.matcher(responseText).replaceAll("").trim();
    }

    private static Optional<ToolInvocation> parseToolCallJson(String json) {
        try {
            String trimmed = json.trim();
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return Optional.empty();
            }

            String name = extractStringField(trimmed, "name");
            if (name == null) {
                return Optional.empty();
            }

            Map<String, Object> arguments = extractObjectField(trimmed, "arguments");
            return Optional.of(new ToolInvocation(name, arguments));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String extractStringField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractObjectField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\\{");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return Map.of();
        }

        int braceStart = matcher.end() - 1;
        int depth = 0;
        int end = -1;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    end = i + 1;
                    break;
                }
            }
        }

        if (end == -1) {
            return Map.of();
        }

        String objectJson = json.substring(braceStart, end);
        return parseSimpleObject(objectJson);
    }

    /**
     * Minimal JSON object parser for flat key-value pairs with string values.
     */
    private static Map<String, Object> parseSimpleObject(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        Pattern kvPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher kvMatcher = kvPattern.matcher(json);
        while (kvMatcher.find()) {
            result.put(kvMatcher.group(1), kvMatcher.group(2));
        }
        return result;
    }
}
