package com.enterprisewebagent.runtime.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TranscriptCompactor {

    private static final int DEFAULT_KEEP_RECENT = 10;
    private static final int COMPACT_KEEP_RECENT = 20;

    public static List<TranscriptEntry> compact(List<TranscriptEntry> entries, CompactionStrategy strategy) {
        return switch (strategy) {
            case CLEAR -> keepRecent(entries, DEFAULT_KEEP_RECENT);
            case COMPACT -> {
                var recent = keepRecent(entries, COMPACT_KEEP_RECENT);
                var oldCount = Math.max(0, entries.size() - COMPACT_KEEP_RECENT);
                if (oldCount > 0) {
                    var old = entries.subList(0, oldCount);
                    String summary = summarizeEntries(old);
                    var result = new ArrayList<TranscriptEntry>();
                    result.add(new TranscriptEntry("system", summary, Instant.now()));
                    result.addAll(recent);
                    yield result;
                }
                yield recent;
            }
            case TRUNCATE -> keepRecent(entries, 2);
        };
    }

    private static List<TranscriptEntry> keepRecent(List<TranscriptEntry> entries, int count) {
        if (entries.size() <= count) return new ArrayList<>(entries);
        return new ArrayList<>(entries.subList(entries.size() - count, entries.size()));
    }

    static String summarizeEntries(List<TranscriptEntry> entries) {
        StringBuilder sb = new StringBuilder("[Compacted conversation summary: ");
        int userCount = 0, assistantCount = 0, toolCount = 0;
        for (var entry : entries) {
            switch (entry.role()) {
                case "user" -> userCount++;
                case "assistant" -> assistantCount++;
                default -> toolCount++;
            }
        }
        sb.append(userCount).append(" user messages, ");
        sb.append(assistantCount).append(" assistant messages, ");
        sb.append(toolCount).append(" tool interactions");
        entries.stream().filter(e -> "user".equals(e.role())).findFirst()
                .ifPresent(e -> sb.append(". First topic: ").append(truncate(e.content(), 100)));
        sb.append("]");
        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
