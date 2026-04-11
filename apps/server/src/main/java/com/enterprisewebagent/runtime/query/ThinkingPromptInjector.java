package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.prompt.PromptSection;

import java.util.Optional;

public class ThinkingPromptInjector {

    private static final String THINKING_INSTRUCTION =
            "Think step by step before responding. Use <thinking>...</thinking> tags for your reasoning. " +
            "Place your complete reasoning inside the thinking tags, then provide your final answer outside the tags.";

    private static final int ADAPTIVE_INPUT_THRESHOLD = 500;

    public Optional<PromptSection> inject(ThinkingConfig config, String userInput) {
        if (config == null || !config.isEnabled()) {
            return Optional.empty();
        }

        return switch (config.mode()) {
            case DISABLED -> Optional.empty();
            case ENABLED -> Optional.of(buildThinkingSection(config));
            case ADAPTIVE -> shouldThink(userInput)
                    ? Optional.of(buildThinkingSection(config))
                    : Optional.empty();
        };
    }

    private PromptSection buildThinkingSection(ThinkingConfig config) {
        String content = THINKING_INSTRUCTION +
                "\nThinking budget: " + config.budgetTokens() + " tokens.";
        return new PromptSection("thinking_instructions", content, false);
    }

    boolean shouldThink(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        if (input.length() > ADAPTIVE_INPUT_THRESHOLD) {
            return true;
        }
        if (input.contains("?") && input.indexOf("?") != input.lastIndexOf("?")) {
            return true;
        }
        String lower = input.toLowerCase();
        if (lower.contains("review") || lower.contains("analyze") || lower.contains("explain") ||
            lower.contains("compare") || lower.contains("debug") || lower.contains("refactor")) {
            return true;
        }
        return false;
    }
}
