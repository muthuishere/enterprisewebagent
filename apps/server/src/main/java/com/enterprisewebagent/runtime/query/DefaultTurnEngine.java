package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.cost.CostCalculator;
import com.enterprisewebagent.runtime.cost.SessionCostTracker;
import com.enterprisewebagent.runtime.cost.TurnCost;
import com.enterprisewebagent.runtime.events.*;
import com.enterprisewebagent.runtime.permissions.DenialTracker;
import com.enterprisewebagent.runtime.permissions.PermissionDecision;
import com.enterprisewebagent.runtime.permissions.PermissionEvaluator;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.provider.ModelProvider;
import com.enterprisewebagent.runtime.provider.ModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.ModelRequest;
import com.enterprisewebagent.runtime.provider.ProviderModels;
import com.enterprisewebagent.runtime.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultTurnEngine implements TurnEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultTurnEngine.class);

    private static final int MAX_TOOL_ITERATIONS = 10;

    private final ModelProviderRegistry providerRegistry;
    private final DefaultToolRegistry toolRegistry;
    private final RuntimeEventPublisher eventPublisher;
    private final PermissionEvaluator permissionEvaluator;
    private final DenialTracker denialTracker;
    private final CostCalculator costCalculator;
    private final SessionCostTracker sessionCostTracker;

    public DefaultTurnEngine(
            ModelProviderRegistry providerRegistry,
            DefaultToolRegistry toolRegistry,
            RuntimeEventPublisher eventPublisher
    ) {
        this(providerRegistry, toolRegistry, eventPublisher, null, null, null, null);
    }

    public DefaultTurnEngine(
            ModelProviderRegistry providerRegistry,
            DefaultToolRegistry toolRegistry,
            RuntimeEventPublisher eventPublisher,
            PermissionEvaluator permissionEvaluator,
            DenialTracker denialTracker,
            CostCalculator costCalculator,
            SessionCostTracker sessionCostTracker
    ) {
        this.providerRegistry = providerRegistry;
        this.toolRegistry = toolRegistry;
        this.eventPublisher = eventPublisher;
        this.permissionEvaluator = permissionEvaluator;
        this.denialTracker = denialTracker;
        this.costCalculator = costCalculator;
        this.sessionCostTracker = sessionCostTracker;
    }

    @Override
    public TurnResult executeTurn(TurnRequest request) {
        String sessionId = request.sessionId();
        log.info("Turn started sessionId={} input_length={}", sessionId, request.input().length());
        eventPublisher.publish(new TurnStartedEvent(sessionId, Instant.now()));

        Instant turnStart = Instant.now();

        try {
            Transcript transcript = new Transcript();
            transcript.addUserMessage(request.input());

            List<PromptSection> conversationPrompt = buildConversationPrompt(request);
            int totalTokenEstimate = 0;
            List<ToolInvocation> allToolCalls = new ArrayList<>();

            String modelResponse = callModel(conversationPrompt, request);
            totalTokenEstimate += estimateTokens(modelResponse);
            log.debug("Model called iteration=0 response_length={}", modelResponse.length());

            int inputTokenEstimate = estimateTokens(request.input());

            for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
                Optional<List<ToolInvocation>> toolCalls = ModelResponseParser.extractToolCalls(modelResponse);

                if (toolCalls.isEmpty()) {
                    break;
                }

                ToolContext toolContext = buildToolContext(request);
                StringBuilder toolResultsText = new StringBuilder();

                for (ToolInvocation invocation : toolCalls.get()) {
                    allToolCalls.add(invocation);
                    log.info("Tool call detected tool={}", invocation.name());

                    // Permission check before execution
                    if (permissionEvaluator != null) {
                        PermissionDecision decision = permissionEvaluator.evaluate(invocation, toolContext);
                        if (!decision.allowed() && !decision.requiresApproval()) {
                            log.warn("Tool denied tool={} reason={}", invocation.name(), decision.reason());
                            if (denialTracker != null) {
                                denialTracker.recordDenial(invocation.name(), decision.reason(), Instant.now());
                            }
                            ToolResult deniedResult = new ToolResult(invocation.name(),
                                    "Permission denied: " + decision.reason(), false);
                            eventPublisher.publish(new ToolCompletedEvent(sessionId, invocation.name(), deniedResult));
                            transcript.addToolResult(invocation.name(), deniedResult.output());
                            toolResultsText.append("[TOOL_RESULT name=\"")
                                    .append(invocation.name())
                                    .append("\"]")
                                    .append(deniedResult.output())
                                    .append("[/TOOL_RESULT]\n");
                            continue;
                        }
                        if (decision.requiresApproval()) {
                            log.info("Tool requires approval tool={} reason={}", invocation.name(), decision.reason());
                            eventPublisher.publish(new AskUserRequestedEvent(
                                    sessionId,
                                    "Permission required to execute " + invocation.name() + ": " + decision.reason(),
                                    List.of("approve", "deny")));
                            ToolResult pendingResult = new ToolResult(invocation.name(),
                                    "PENDING_APPROVAL: " + decision.reason(), false);
                            eventPublisher.publish(new ToolCompletedEvent(sessionId, invocation.name(), pendingResult));
                            transcript.addToolResult(invocation.name(), pendingResult.output());
                            toolResultsText.append("[TOOL_RESULT name=\"")
                                    .append(invocation.name())
                                    .append("\"]")
                                    .append(pendingResult.output())
                                    .append("[/TOOL_RESULT]\n");
                            continue;
                        }
                    }

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

                // Append tool results as a new section and call model again
                List<PromptSection> updatedPrompt = new ArrayList<>(conversationPrompt);
                updatedPrompt.add(new PromptSection("tool_results", toolResultsText.toString(), false));
                conversationPrompt = updatedPrompt;

                modelResponse = callModel(conversationPrompt, request);
                totalTokenEstimate += estimateTokens(modelResponse);
                log.debug("Model called iteration={} response_length={}", iteration + 1, modelResponse.length());
            }

            String finalOutput = ModelResponseParser.extractTextContent(modelResponse);
            transcript.addAssistantMessage(finalOutput);

            log.info("Turn completed sessionId={} iterations={} output_length={}",
                    sessionId, allToolCalls.size(), finalOutput.length());
            eventPublisher.publish(new TurnCompletedEvent(sessionId, finalOutput, Instant.now()));

            // Record cost
            if (costCalculator != null && sessionCostTracker != null) {
                Duration apiDuration = Duration.between(turnStart, Instant.now());
                String model = request.model() != null ? request.model() : "unknown";
                TurnCost turnCost = costCalculator.calculate(model, inputTokenEstimate,
                        totalTokenEstimate, 0, apiDuration);
                sessionCostTracker.recordTurnCost(sessionId, turnCost);
            }

            return new TurnResult(
                    sessionId,
                    finalOutput,
                    List.copyOf(allToolCalls),
                    true,
                    List.copyOf(transcript.entries()),
                    totalTokenEstimate
            );
        } catch (Exception e) {
            log.warn("Turn failed sessionId={} error={}", sessionId, e.getMessage());
            eventPublisher.publish(new TurnFailedEvent(sessionId, e.getMessage(), Instant.now()));
            return new TurnResult(sessionId, e.getMessage(), List.of(), false, List.of(), 0);
        }
    }

    private List<PromptSection> buildConversationPrompt(TurnRequest request) {
        List<PromptSection> prompt = new ArrayList<>(request.effectivePrompt());

        // Append prior transcript entries as context
        if (request.transcript() != null && !request.transcript().isEmpty()) {
            StringBuilder transcriptText = new StringBuilder();
            for (TranscriptEntry entry : request.transcript()) {
                transcriptText.append(entry.role()).append(": ").append(entry.content()).append("\n");
            }
            prompt.add(new PromptSection("transcript", transcriptText.toString(), false));
        }

        // Append current user input
        prompt.add(new PromptSection("user_input", request.input(), false));

        return prompt;
    }

    private String callModel(List<PromptSection> prompt, TurnRequest request) {
        ModelProvider provider = resolveProvider(request.model());
        String actualModel = ProviderModels.resolveModelName(request.model());
        ModelRequest modelRequest = new ModelRequest(
                prompt,
                actualModel,
                request.options() != null ? request.options() : Map.of()
        );
        return provider.complete(modelRequest);
    }

    private ModelProvider resolveProvider(String model) {
        if (model == null || model.isBlank()) {
            return providerRegistry.getProvider(null);
        }
        String providerId = ProviderModels.resolveProvider(model);
        if (providerId == null) {
            return providerRegistry.getProvider(null);
        }
        try {
            return providerRegistry.getProvider(providerId);
        } catch (IllegalArgumentException e) {
            log.warn("Provider '{}' not registered, falling back to default", providerId);
            return providerRegistry.getProvider(null);
        }
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
