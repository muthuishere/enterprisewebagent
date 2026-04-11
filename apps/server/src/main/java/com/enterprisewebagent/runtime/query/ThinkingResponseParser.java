package com.enterprisewebagent.runtime.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThinkingResponseParser {

    private static final Pattern THINKING_PATTERN =
            Pattern.compile("<thinking>(.*?)</thinking>", Pattern.DOTALL);

    private ThinkingResponseParser() {}

    public static ThinkingResult parse(String responseText) {
        if (responseText == null || responseText.isEmpty()) {
            return new ThinkingResult(null, "");
        }

        Matcher matcher = THINKING_PATTERN.matcher(responseText);
        StringBuilder thinkingContent = new StringBuilder();
        boolean found = false;

        while (matcher.find()) {
            found = true;
            if (!thinkingContent.isEmpty()) {
                thinkingContent.append("\n");
            }
            thinkingContent.append(matcher.group(1).trim());
        }

        String answer = THINKING_PATTERN.matcher(responseText).replaceAll("").trim();

        return new ThinkingResult(
                found ? thinkingContent.toString() : null,
                answer
        );
    }

    public static boolean containsThinking(String responseText) {
        if (responseText == null) {
            return false;
        }
        return THINKING_PATTERN.matcher(responseText).find();
    }

    public record ThinkingResult(String thinkingContent, String answer) {
        public boolean hasThinking() {
            return thinkingContent != null && !thinkingContent.isEmpty();
        }
    }
}
