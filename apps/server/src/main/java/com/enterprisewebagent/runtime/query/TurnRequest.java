package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.tools.ToolDefinition;

import java.util.List;
import java.util.Map;

public record TurnRequest(
    String sessionId,
    String input,
    List<PromptSection> effectivePrompt,
    List<ToolDefinition> availableTools,
    List<TranscriptEntry> transcript,
    String model,
    Map<String, Object> options
) {
    public TurnRequest(String sessionId, String input, List<PromptSection> effectivePrompt, List<ToolDefinition> availableTools) {
        this(sessionId, input, effectivePrompt, availableTools, List.of(), null, Map.of());
    }
}
