package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.events.*;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.provider.ModelProvider;
import com.enterprisewebagent.runtime.provider.ModelRequest;
import com.enterprisewebagent.runtime.tools.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Streaming variant of the turn engine. Uses modelProvider.stream()
 * and publishes TokenDeltaEvent for each chunk. After collecting the
 * full response, tool-call detection proceeds identically to DefaultTurnEngine.
 */
public class StreamingTurnEngine implements TurnEngine {

    private static final int MAX_TOOL_ITERATIONS = 10;

    private final ModelProvider modelProvider;
    private final DefaultToolRegistry toolRegistry;
    private final RuntimeEventPublisher eventPublisher;

    public StreamingTurnEngine(
            ModelProvider modelProvider,
            DefaultToolRegistry toolRegistry,
            RuntimeEventPublisher eventPublisher
    ) {
        this.modelProvider = modelProvider;
        this.toolRegistry = toolRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TurnResult executeTurn(TurnRequest request) {
        String sessionId = request.sessionId();
        eventPublisher.publish(new TurnStartedEvent(sessionId, Instant.now()));

        try {
            Transcript transcript = new Transcript();
            transcript.addUserMessage(request.input());

            List<PromptSection> conversationPrompt = buildConversationPrompt(request);
            int totalTokenEstimate = 0;
            List<ToolInvocation> allToolCalls = new ArrayList<>();

            String modelResponse = streamModel(conversationPrompt, request, sessionId);
            totalTokenEstimate += estimateTokens(modelResponse);

            for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
                Optional<List<ToolInvocation>> toolCalls = ModelResponseParser.extractToolCalls(modelResponse);

                if (toolCalls.isEmpty()) {
                    break;
                }

                ToolContext toolContext = buildToolContext(request);
                StringBuilder toolResultsText = new StringBuilder();

                for (ToolInvocation invocation : toolCalls.get()) {
                    allToolCalls.add(invocation);
                    eventPublisher.publish(new ToolRequestedEvent(sessionId, invocation.name(), invocation.arguments()));

                    transcript.addToolCall(invocation.name(), invocation.arguments().toString());

                    ToolResult toolResult = toolRegistry.execute(invocation, toolContext);
                    eventPublisher.publish(new ToolCompletedEvent(sessionId, invocation.name(), toolResult));

                    transcript.addToolResult(invocation.name(), toolResult.output());

                    toolResultsText.append("[TOOL_RESULT name=\"")
                            .append(invocation.name())
                            .append("\"]")
                            .append(toolResult.output())
                            .append("[/TOOL_RESULT]\n");
                }

                String textSoFar = ModelResponseParser.extractTextContent(modelResponse);
                transcript.addAssistantMessage(textSoFar);

                List<PromptSection> updatedPrompt = new ArrayList<>(conversationPrompt);
                updatedPrompt.add(new PromptSection("tool_results", toolResultsText.toString(), false));
                conversationPrompt = updatedPrompt;

                // Subsequent calls also stream
                modelResponse = streamModel(conversationPrompt, request, sessionId);
                totalTokenEstimate += estimateTokens(modelResponse);
            }

            String finalOutput = ModelResponseParser.extractTextContent(modelResponse);
            transcript.addAssistantMessage(finalOutput);

            eventPublisher.publish(new TurnCompletedEvent(sessionId, finalOutput, Instant.now()));

            return new TurnResult(
                    sessionId,
                    finalOutput,
                    List.copyOf(allToolCalls),
                    true,
                    List.copyOf(transcript.entries()),
                    totalTokenEstimate
            );
        } catch (Exception e) {
            eventPublisher.publish(new TurnFailedEvent(sessionId, e.getMessage(), Instant.now()));
            return new TurnResult(sessionId, e.getMessage(), List.of(), false, List.of(), 0);
        }
    }

    private String streamModel(List<PromptSection> prompt, TurnRequest request, String sessionId) {
        ModelRequest modelRequest = new ModelRequest(
                prompt,
                request.model(),
                request.options() != null ? request.options() : Map.of()
        );

        StringBuilder collected = new StringBuilder();
        modelProvider.stream(modelRequest)
                .doOnNext(chunk -> {
                    eventPublisher.publish(new TokenDeltaEvent(sessionId, chunk));
                    collected.append(chunk);
                })
                .blockLast();

        return collected.toString();
    }

    private List<PromptSection> buildConversationPrompt(TurnRequest request) {
        List<PromptSection> prompt = new ArrayList<>(request.effectivePrompt());

        if (request.transcript() != null && !request.transcript().isEmpty()) {
            StringBuilder transcriptText = new StringBuilder();
            for (TranscriptEntry entry : request.transcript()) {
                transcriptText.append(entry.role()).append(": ").append(entry.content()).append("\n");
            }
            prompt.add(new PromptSection("transcript", transcriptText.toString(), false));
        }

        prompt.add(new PromptSection("user_input", request.input(), false));

        return prompt;
    }

    private ToolContext buildToolContext(TurnRequest request) {
        Set<String> capabilities = request.availableTools().stream()
                .map(ToolDefinition::name)
                .collect(Collectors.toSet());
        return new ToolContext(request.sessionId(), "normal", capabilities);
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }
}
