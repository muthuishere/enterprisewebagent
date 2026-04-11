package com.enterprisewebagent.runtime.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Transcript {

    private final List<TranscriptEntry> entries = new ArrayList<>();

    public void addUserMessage(String input) {
        entries.add(new TranscriptEntry("user", input, Instant.now()));
    }

    public void addAssistantMessage(String output) {
        entries.add(new TranscriptEntry("assistant", output, Instant.now()));
    }

    public void addToolCall(String toolName, String arguments) {
        entries.add(new TranscriptEntry("tool_call", toolName + ": " + arguments, Instant.now()));
    }

    public void addToolResult(String toolName, String result) {
        entries.add(new TranscriptEntry("tool_result", toolName + ": " + result, Instant.now()));
    }

    public List<TranscriptEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public List<TranscriptEntry> recentEntries(int maxEntries) {
        if (maxEntries >= entries.size()) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
        int fromIndex = entries.size() - maxEntries;
        return Collections.unmodifiableList(new ArrayList<>(entries.subList(fromIndex, entries.size())));
    }

    public void replaceAll(List<TranscriptEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
    }

    public int estimateTokens() {
        return PromptBudgetCalculator.estimateTranscriptTokens(entries);
    }

    public int size() {
        return entries.size();
    }
}
