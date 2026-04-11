package com.enterprisewebagent.runtime.memory;

import com.enterprisewebagent.runtime.query.TranscriptEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AutoMemorySummarizer {

    private static final Pattern DECISION_PATTERN = Pattern.compile(
            "(?i)(decided|chosen|selected|agreed|will use|switched to|going with)", Pattern.MULTILINE);
    private static final Pattern ERROR_PATTERN = Pattern.compile(
            "(?i)(error|exception|failed|bug|fix|issue|problem)", Pattern.MULTILINE);
    private static final Pattern FILE_CHANGE_PATTERN = Pattern.compile(
            "(?i)(created|modified|deleted|renamed|moved|updated|wrote|edited)\\s+[`'\"]?[\\w/.-]+", Pattern.MULTILINE);

    public String summarize(List<TranscriptEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        List<String> decisions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> fileChanges = new ArrayList<>();

        for (TranscriptEntry entry : entries) {
            String content = entry.content();
            if (content == null || content.isBlank()) continue;

            extractMatches(content, DECISION_PATTERN, decisions, "decision");
            extractMatches(content, ERROR_PATTERN, errors, "error");
            extractMatches(content, FILE_CHANGE_PATTERN, fileChanges, "file_change");
        }

        var sb = new StringBuilder();

        if (!decisions.isEmpty()) {
            sb.append("### Decisions\n");
            for (String d : decisions) {
                sb.append("- ").append(d).append('\n');
            }
            sb.append('\n');
        }

        if (!errors.isEmpty()) {
            sb.append("### Errors Encountered\n");
            for (String e : errors) {
                sb.append("- ").append(e).append('\n');
            }
            sb.append('\n');
        }

        if (!fileChanges.isEmpty()) {
            sb.append("### File Changes\n");
            for (String f : fileChanges) {
                sb.append("- ").append(f).append('\n');
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    private void extractMatches(String content, Pattern pattern, List<String> results, String category) {
        var matcher = pattern.matcher(content);
        while (matcher.find()) {
            int start = Math.max(0, matcher.start() - 40);
            int end = Math.min(content.length(), matcher.end() + 60);

            String lineStart = content.substring(start, end).trim();
            int newline = lineStart.indexOf('\n');
            if (newline > 0) {
                lineStart = lineStart.substring(0, newline);
            }

            if (lineStart.length() > 120) {
                lineStart = lineStart.substring(0, 120) + "...";
            }

            if (!results.contains(lineStart) && results.size() < 10) {
                results.add(lineStart);
            }
        }
    }
}
