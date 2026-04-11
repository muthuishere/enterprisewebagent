package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.tools.ToolInvocation;

import java.util.List;

public record TurnResult(
    String sessionId,
    String output,
    List<ToolInvocation> toolCalls,
    boolean completed,
    List<TranscriptEntry> newTranscriptEntries,
    int tokensUsed
) {
    public TurnResult(String sessionId, String output, List<ToolInvocation> toolCalls, boolean completed) {
        this(sessionId, output, toolCalls, completed, List.of(), 0);
    }
}
