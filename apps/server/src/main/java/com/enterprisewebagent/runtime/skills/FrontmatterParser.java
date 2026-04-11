package com.enterprisewebagent.runtime.skills;

import java.util.LinkedHashMap;
import java.util.Map;

public class FrontmatterParser {

    public record ParseResult(Map<String, String> frontmatter, String content) {}

    public static ParseResult parse(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return new ParseResult(Map.of(), markdown != null ? markdown : "");
        }

        if (!markdown.startsWith("---\n") && !markdown.startsWith("---\r\n")) {
            return new ParseResult(Map.of(), markdown);
        }

        int startDelimiterEnd = markdown.indexOf('\n') + 1;
        int closingIndex = findClosingDelimiter(markdown, startDelimiterEnd);
        if (closingIndex == -1) {
            return new ParseResult(Map.of(), markdown);
        }

        String frontmatterBlock = markdown.substring(startDelimiterEnd, closingIndex);
        Map<String, String> frontmatter = parseFrontmatterBlock(frontmatterBlock);

        int contentStart = markdown.indexOf('\n', closingIndex) + 1;
        String content = contentStart > 0 ? markdown.substring(contentStart) : "";

        return new ParseResult(frontmatter, content);
    }

    private static int findClosingDelimiter(String markdown, int fromIndex) {
        int idx = fromIndex;
        while (idx < markdown.length()) {
            int lineEnd = markdown.indexOf('\n', idx);
            String line;
            if (lineEnd == -1) {
                line = markdown.substring(idx);
            } else {
                line = markdown.substring(idx, lineEnd);
            }
            if (line.trim().equals("---")) {
                return idx;
            }
            if (lineEnd == -1) {
                break;
            }
            idx = lineEnd + 1;
        }
        return -1;
    }

    private static Map<String, String> parseFrontmatterBlock(String block) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : block.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > 0) {
                String key = trimmed.substring(0, colonIndex).trim();
                String value = trimmed.substring(colonIndex + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }
}
