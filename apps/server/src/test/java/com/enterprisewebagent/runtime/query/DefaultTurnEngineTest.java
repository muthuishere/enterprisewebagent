package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.events.*;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.ModelProvider;
import com.enterprisewebagent.runtime.provider.ModelRequest;
import com.enterprisewebagent.runtime.tools.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTurnEngineTest {

    private DefaultToolRegistry toolRegistry;
    private InMemoryEventPublisher eventPublisher;
    private List<RuntimeEvent> capturedEvents;

    @BeforeEach
    void setUp() {
        toolRegistry = new DefaultToolRegistry();
        eventPublisher = new InMemoryEventPublisher();
        capturedEvents = new CopyOnWriteArrayList<>();
        eventPublisher.addListener(capturedEvents::add);
    }

    @Test
    void simpleTurnWithNoToolCalls() {
        var engine = new DefaultTurnEngine(
                registryWith("stub", stubProvider("Hello, how can I help?")),
                toolRegistry,
                eventPublisher
        );

        TurnResult result = engine.executeTurn(minimalRequest("Hi"));

        assertTrue(result.completed());
        assertEquals("Hello, how can I help?", result.output());
        assertTrue(result.toolCalls().isEmpty());
        assertTrue(result.tokensUsed() > 0);
        assertFalse(result.newTranscriptEntries().isEmpty());
    }

    @Test
    void turnWithOneToolCallExecutesTool() {
        toolRegistry.registerExecutor(new StubToolExecutor("file_read", "file contents here"));

        var engine = new DefaultTurnEngine(
                registryWith("stub", stubProvider(
                        "[TOOL_CALL]{\"name\":\"file_read\", \"arguments\":{\"path\":\"/a.txt\"}}[/TOOL_CALL]",
                        "The file contains: file contents here"
                )),
                toolRegistry,
                eventPublisher
        );

        TurnResult result = engine.executeTurn(minimalRequest("Read the file"));

        assertTrue(result.completed());
        assertEquals("The file contains: file contents here", result.output());
        assertEquals(1, result.toolCalls().size());
        assertEquals("file_read", result.toolCalls().getFirst().name());
    }

    @Test
    void turnFailurePublishesTurnFailedEvent() {
        ModelProvider failingProvider = new ModelProvider() {
            @Override
            public String complete(ModelRequest r) { throw new RuntimeException("Model unavailable"); }
            @Override
            public Flux<String> stream(ModelRequest r) { return Flux.error(new RuntimeException("Model unavailable")); }
        };

        var engine = new DefaultTurnEngine(
                registryWith("stub", failingProvider), toolRegistry, eventPublisher);

        TurnResult result = engine.executeTurn(minimalRequest("Hello"));

        assertFalse(result.completed());
        assertTrue(result.output().contains("Model unavailable"));

        boolean hasFailed = capturedEvents.stream().anyMatch(e -> e instanceof TurnFailedEvent);
        assertTrue(hasFailed, "Expected TurnFailedEvent to be published");
    }

    @Test
    void maxIterationsPreventInfiniteLoop() {
        String toolCallResponse = "[TOOL_CALL]{\"name\":\"shell\", \"arguments\":{\"command\":\"echo hi\"}}[/TOOL_CALL]";
        String[] responses = new String[12];
        Arrays.fill(responses, toolCallResponse);
        responses[10] = "Final answer after limit";
        responses[11] = "Should not reach here";

        toolRegistry.registerExecutor(new StubToolExecutor("shell", "hi"));

        var engine = new DefaultTurnEngine(
                registryWith("stub", stubProvider(responses)),
                toolRegistry,
                eventPublisher
        );

        TurnResult result = engine.executeTurn(minimalRequest("Run forever"));

        assertTrue(result.completed());
        assertTrue(result.toolCalls().size() <= 10,
                "Expected at most 10 tool calls but got " + result.toolCalls().size());
    }

    @Test
    void eventsPublishedInCorrectOrder() {
        toolRegistry.registerExecutor(new StubToolExecutor("shell", "output"));

        var engine = new DefaultTurnEngine(
                registryWith("stub", stubProvider(
                        "[TOOL_CALL]{\"name\":\"shell\", \"arguments\":{\"command\":\"ls\"}}[/TOOL_CALL]",
                        "Here are the files."
                )),
                toolRegistry,
                eventPublisher
        );

        engine.executeTurn(minimalRequest("List files"));

        assertTrue(capturedEvents.size() >= 4,
                "Expected at least 4 events but got " + capturedEvents.size());
        assertInstanceOf(TurnStartedEvent.class, capturedEvents.getFirst());
        assertInstanceOf(ToolRequestedEvent.class, capturedEvents.get(1));
        assertInstanceOf(ToolCompletedEvent.class, capturedEvents.get(2));
        assertInstanceOf(TurnCompletedEvent.class, capturedEvents.getLast());
    }

    @Test
    void backwardCompatibleTurnRequestWorks() {
        var request = new TurnRequest("s1", "hi", List.of(), List.of());
        assertEquals("s1", request.sessionId());
        assertNotNull(request.transcript());
        assertTrue(request.transcript().isEmpty());
        assertNull(request.model());
        assertNotNull(request.options());
    }

    @Test
    void backwardCompatibleTurnResultWorks() {
        var result = new TurnResult("s1", "out", List.of(), true);
        assertEquals("s1", result.sessionId());
        assertTrue(result.newTranscriptEntries().isEmpty());
        assertEquals(0, result.tokensUsed());
    }

    @Test
    void nullModelRoutesToDefaultProvider() {
        var engine = new DefaultTurnEngine(
                registryWith("stub", stubProvider("default response")),
                toolRegistry,
                eventPublisher
        );

        TurnResult result = engine.executeTurn(minimalRequest("Hi"));
        assertTrue(result.completed());
        assertEquals("default response", result.output());
    }

    @Test
    void prefixedModelRoutesToCorrectProvider() {
        var copilotProvider = stubProvider("copilot response");
        var defaultProvider = stubProvider("default response");

        DefaultModelProviderRegistry registry = new DefaultModelProviderRegistry();
        registry.register("stub", defaultProvider);
        registry.register("copilot", copilotProvider);
        registry.setDefault("stub");

        var engine = new DefaultTurnEngine(registry, toolRegistry, eventPublisher);

        TurnRequest request = new TurnRequest("s1", "Hello", List.of(), List.of(),
                List.of(), "copilot:gpt-4.1", Map.of());
        TurnResult result = engine.executeTurn(request);

        assertTrue(result.completed());
        assertEquals("copilot response", result.output());
    }

    @Test
    void ollamaModelRoutesToOllamaProvider() {
        var ollamaProvider = stubProvider("ollama response");
        var defaultProvider = stubProvider("default response");

        DefaultModelProviderRegistry registry = new DefaultModelProviderRegistry();
        registry.register("stub", defaultProvider);
        registry.register("ollama", ollamaProvider);
        registry.setDefault("stub");

        var engine = new DefaultTurnEngine(registry, toolRegistry, eventPublisher);

        TurnRequest request = new TurnRequest("s1", "Hello", List.of(), List.of(),
                List.of(), "ollama:llama3.2", Map.of());
        TurnResult result = engine.executeTurn(request);

        assertTrue(result.completed());
        assertEquals("ollama response", result.output());
    }

    @Test
    void unknownProviderFallsBackToDefault() {
        var defaultProvider = stubProvider("default response");

        DefaultModelProviderRegistry registry = new DefaultModelProviderRegistry();
        registry.register("stub", defaultProvider);
        registry.setDefault("stub");

        var engine = new DefaultTurnEngine(registry, toolRegistry, eventPublisher);

        TurnRequest request = new TurnRequest("s1", "Hello", List.of(), List.of(),
                List.of(), "nonexistent:some-model", Map.of());
        TurnResult result = engine.executeTurn(request);

        assertTrue(result.completed());
        assertEquals("default response", result.output());
    }

    // --- helpers ---

    private TurnRequest minimalRequest(String input) {
        return new TurnRequest(
                "test-session",
                input,
                List.of(new PromptSection("system", "You are a helpful assistant.", false)),
                List.of()
        );
    }

    private static DefaultModelProviderRegistry registryWith(String id, ModelProvider provider) {
        DefaultModelProviderRegistry registry = new DefaultModelProviderRegistry();
        registry.register(id, provider);
        registry.setDefault(id);
        return registry;
    }

    private static ModelProvider stubProvider(String... responses) {
        return new StubModelProvider(responses);
    }

    static class StubModelProvider implements ModelProvider {
        private final Queue<String> responses;

        StubModelProvider(String... responses) {
            this.responses = new LinkedList<>(List.of(responses));
        }

        @Override
        public String complete(ModelRequest r) {
            String next = responses.poll();
            if (next == null) throw new IllegalStateException("No more stub responses");
            return next;
        }

        @Override
        public Flux<String> stream(ModelRequest r) {
            String next = responses.poll();
            if (next == null) return Flux.error(new IllegalStateException("No more stub responses"));
            return Flux.just(next.split(""));
        }
    }

    static class StubToolExecutor implements ToolExecutor {
        private final String name;
        private final String output;

        StubToolExecutor(String name, String output) {
            this.name = name;
            this.output = output;
        }

        @Override
        public ToolResult execute(ToolInvocation invocation, ToolContext context) {
            return new ToolResult(name, output, true);
        }

        @Override
        public String toolName() {
            return name;
        }
    }
}
