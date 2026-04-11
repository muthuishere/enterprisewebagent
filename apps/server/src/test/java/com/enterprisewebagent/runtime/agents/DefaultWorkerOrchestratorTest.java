package com.enterprisewebagent.runtime.agents;

import com.enterprisewebagent.runtime.events.RuntimeEvent;
import com.enterprisewebagent.runtime.events.RuntimeEventPublisher;
import com.enterprisewebagent.runtime.events.WorkerStateChangedEvent;
import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptContext;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.query.TurnRequest;
import com.enterprisewebagent.runtime.query.TurnResult;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultWorkerOrchestratorTest {

    private StubTurnEngine turnEngine;
    private StubPromptAssembler promptAssembler;
    private StubToolRegistry toolRegistry;
    private RecordingEventPublisher eventPublisher;
    private DefaultWorkerOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        turnEngine = new StubTurnEngine("worker output", true);
        promptAssembler = new StubPromptAssembler();
        toolRegistry = new StubToolRegistry();
        eventPublisher = new RecordingEventPublisher();
        orchestrator = new DefaultWorkerOrchestrator(turnEngine, promptAssembler, toolRegistry, eventPublisher);
    }

    @Test
    void delegate_successfulExecution_returnsSuccessResult() {
        var worker = AgentDefinitionFactory.generalWorker("w1", "prompt");
        var ctx = new AgentContext("sess-1", AgentRole.COORDINATOR, Map.of());

        WorkerResult result = orchestrator.delegate(worker, "do something", ctx);

        assertTrue(result.success());
        assertEquals("w1", result.workerId());
        assertEquals("worker output", result.output());
    }

    @Test
    void delegate_maxDepthExceeded_returnsFailure() {
        var worker = AgentDefinitionFactory.generalWorker("w1", "prompt");
        var ctx = new AgentContext("sess-1", AgentRole.COORDINATOR, Map.of(), null, 3, 3);

        WorkerResult result = orchestrator.delegate(worker, "do something", ctx);

        assertFalse(result.success());
        assertEquals("Max nesting depth exceeded", result.output());
    }

    @Test
    void delegate_publishesEventsInOrder_startedThenCompleted() {
        var worker = AgentDefinitionFactory.generalWorker("w1", "prompt");
        var ctx = new AgentContext("sess-1", AgentRole.COORDINATOR, Map.of());

        orchestrator.delegate(worker, "task", ctx);

        assertEquals(2, eventPublisher.events.size());
        var started = (WorkerStateChangedEvent) eventPublisher.events.get(0);
        var completed = (WorkerStateChangedEvent) eventPublisher.events.get(1);
        assertEquals("started", started.newState());
        assertEquals("completed", completed.newState());
        assertEquals("w1", started.workerId());
        assertEquals("sess-1", started.sessionId());
    }

    @Test
    void delegate_turnNotCompleted_publishesFailedState() {
        turnEngine = new StubTurnEngine("partial", false);
        orchestrator = new DefaultWorkerOrchestrator(turnEngine, promptAssembler, toolRegistry, eventPublisher);

        var worker = AgentDefinitionFactory.generalWorker("w1", "prompt");
        var ctx = new AgentContext("sess-1", AgentRole.COORDINATOR, Map.of());

        WorkerResult result = orchestrator.delegate(worker, "task", ctx);

        assertFalse(result.success());
        var lastEvent = (WorkerStateChangedEvent) eventPublisher.events.get(eventPublisher.events.size() - 1);
        assertEquals("failed", lastEvent.newState());
    }

    @Test
    void delegate_exceptionDuringTurn_returnsFailureAndPublishesFailed() {
        var failingEngine = new FailingTurnEngine("boom");
        orchestrator = new DefaultWorkerOrchestrator(failingEngine, promptAssembler, toolRegistry, eventPublisher);

        var worker = AgentDefinitionFactory.generalWorker("w1", "prompt");
        var ctx = new AgentContext("sess-1", AgentRole.COORDINATOR, Map.of());

        WorkerResult result = orchestrator.delegate(worker, "task", ctx);

        assertFalse(result.success());
        assertEquals("boom", result.output());
        var lastEvent = (WorkerStateChangedEvent) eventPublisher.events.get(eventPublisher.events.size() - 1);
        assertEquals("failed", lastEvent.newState());
    }

    @Test
    void delegate_workerGetsScopedTools() {
        var worker = AgentDefinitionFactory.exploreWorker("e1", "explore");
        var ctx = new AgentContext("sess-1", AgentRole.COORDINATOR, Map.of());

        orchestrator.delegate(worker, "search", ctx);

        assertNotNull(toolRegistry.lastContext);
        assertEquals("worker", toolRegistry.lastContext.mode());
        assertEquals(worker.allowedTools(), toolRegistry.lastContext.capabilities());
    }

    // --- Stubs ---

    static class StubTurnEngine implements TurnEngine {
        private final String response;
        private final boolean completed;

        StubTurnEngine(String response, boolean completed) {
            this.response = response;
            this.completed = completed;
        }

        @Override
        public TurnResult executeTurn(TurnRequest req) {
            return new TurnResult(req.sessionId(), response, List.of(), completed);
        }
    }

    static class FailingTurnEngine implements TurnEngine {
        private final String message;

        FailingTurnEngine(String message) { this.message = message; }

        @Override
        public TurnResult executeTurn(TurnRequest req) {
            throw new RuntimeException(message);
        }
    }

    static class StubPromptAssembler implements PromptAssembler {
        @Override
        public List<PromptSection> assemble(PromptContext ctx) {
            return List.of(new PromptSection("test", "test prompt", true));
        }
    }

    static class StubToolRegistry implements ToolRegistry {
        ToolContext lastContext;

        @Override
        public List<ToolDefinition> resolveTools(ToolContext context) {
            this.lastContext = context;
            return List.of(new ToolDefinition("file_read", "Read files", Map.of(), true));
        }

        @Override
        public void register(ToolDefinition tool) {}

        @Override
        public void deny(String toolName) {}
    }

    static class RecordingEventPublisher implements RuntimeEventPublisher {
        final List<RuntimeEvent> events = new ArrayList<>();

        @Override
        public void publish(RuntimeEvent event) {
            events.add(event);
        }
    }
}
