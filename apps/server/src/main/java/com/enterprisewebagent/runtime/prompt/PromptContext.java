package com.enterprisewebagent.runtime.prompt;

import java.util.Map;

public record PromptContext(
    String sessionId,
    PromptPrecedence activePrecedence,
    Map<String, String> dynamicSections,
    String overridePrompt,
    String coordinatorPrompt,
    String customAgentPrompt,
    String userSystemPrompt,
    String appendPrompt,
    String memoryPrompt
) {
    /** Backward-compatible constructor for existing call sites. */
    public PromptContext(String sessionId, PromptPrecedence activePrecedence, Map<String, String> dynamicSections) {
        this(sessionId, activePrecedence, dynamicSections, null, null, null, null, null, null);
    }
}
