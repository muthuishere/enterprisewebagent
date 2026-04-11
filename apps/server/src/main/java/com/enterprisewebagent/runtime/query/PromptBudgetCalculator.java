package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.prompt.PromptSection;

import java.util.List;

public class PromptBudgetCalculator {

    private static final int DEFAULT_MAX_PROMPT_TOKENS = 100_000;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 8_000;
    private static final int CHARS_PER_TOKEN = 4;

    public record BudgetCheck(boolean withinBudget, int estimatedTokens, int maxTokens, String recommendation) {}

    public static int estimateTokens(List<PromptSection> sections) {
        return sections.stream()
                .mapToInt(s -> s.content().length() / CHARS_PER_TOKEN)
                .sum();
    }

    public static int estimateTranscriptTokens(List<TranscriptEntry> entries) {
        return entries.stream()
                .mapToInt(e -> e.content().length() / CHARS_PER_TOKEN)
                .sum();
    }

    public static BudgetCheck checkBudget(List<PromptSection> sections, List<TranscriptEntry> transcript) {
        int promptTokens = estimateTokens(sections);
        int transcriptTokens = estimateTranscriptTokens(transcript);
        int total = promptTokens + transcriptTokens;

        if (total > DEFAULT_MAX_PROMPT_TOKENS) {
            return new BudgetCheck(false, total, DEFAULT_MAX_PROMPT_TOKENS,
                    "PROMPT_TOO_LONG: Consider compaction strategy COMPACT or TRUNCATE");
        }
        if (total > DEFAULT_MAX_PROMPT_TOKENS * 0.8) {
            return new BudgetCheck(true, total, DEFAULT_MAX_PROMPT_TOKENS,
                    "APPROACHING_LIMIT: Consider proactive compaction");
        }
        return new BudgetCheck(true, total, DEFAULT_MAX_PROMPT_TOKENS, "OK");
    }

    public static BudgetCheck checkOutputBudget(int estimatedOutputTokens) {
        if (estimatedOutputTokens > DEFAULT_MAX_OUTPUT_TOKENS) {
            return new BudgetCheck(false, estimatedOutputTokens, DEFAULT_MAX_OUTPUT_TOKENS,
                    "OUTPUT_TOO_LONG: Truncate or summarize output");
        }
        return new BudgetCheck(true, estimatedOutputTokens, DEFAULT_MAX_OUTPUT_TOKENS, "OK");
    }
}
