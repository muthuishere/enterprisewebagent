package com.enterprisewebagent.runtime.agents;

import com.enterprisewebagent.runtime.tools.ToolInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoordinatorRuntimeTest {

    private RecordingOrchestrator orchestrator;
    private CoordinatorRuntime runtime;

    @BeforeEach
    void setUp() {
        orchestrator = new RecordingOrchestrator("result");
        runtime = new CoordinatorRuntime(orchestrator, "sess-1");
    }

    @Test
    void runWorker_delegatesToOrchestrator() {
        var worker = AgentDefinitionFactory.generalWorker("w1", "prompt");

        WorkerResult result = runtime.runWorker(worker, "do task");

        assertTrue(result.success());
        assertEquals("result", result.output());
        assertEquals("w1", result.workerId());
    }

    @Test
    void completedWorkers_tracksResults() {
        var w1 = AgentDefinitionFactory.generalWorker("w1", "p1");
        var w2 = AgentDefinitionFactory.exploreWorker("w2", "p2");

        runtime.runWorker(w1, "task 1");
        runtime.runWorker(w2, "task 2");

        List<WorkerResult> completed = runtime.completedWorkers();
        assertEquals(2, completed.size());
        assertEquals("w1", completed.get(0).workerId());
        assertEquals("w2", completed.get(1).workerId());
    }

    @Test
    void completedWorkers_returnsUnmodifiableList() {
        var worker = AgentDefinitionFactory.generalWorker("w1", "prompt");
        runtime.runWorker(worker, "task");

        List<WorkerResult> completed = runtime.completedWorkers();
        assertThrows(UnsupportedOperationException.class, () -> completed.add(
            new WorkerResult("x", "x", true, List.of())));
    }

    @Test
    void runParallel_returnsAllResults() {
        var w1 = AgentDefinitionFactory.generalWorker("w1", "p1");
        var w2 = AgentDefinitionFactory.exploreWorker("w2", "p2");
        var w3 = AgentDefinitionFactory.planWorker("w3", "p3");

        List<Map.Entry<AgentDefinition, String>> tasks = List.of(
            Map.entry(w1, "task 1"),
            Map.entry(w2, "task 2"),
            Map.entry(w3, "task 3")
        );

        List<WorkerResult> results = runtime.runParallel(tasks);

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(WorkerResult::success));
    }

    @Test
    void runParallel_tracksInCompletedWorkers() {
        var w1 = AgentDefinitionFactory.generalWorker("w1", "p1");

        runtime.runParallel(List.of(Map.entry(w1, "task")));

        assertEquals(1, runtime.completedWorkers().size());
    }

    @Test
    void runParallel_handlesOrchestratorException() {
        var failing = new FailingOrchestrator();
        var failRuntime = new CoordinatorRuntime(failing, "sess-err");
        var worker = AgentDefinitionFactory.generalWorker("w1", "p");

        List<WorkerResult> results = failRuntime.runParallel(List.of(Map.entry(worker, "task")));

        assertEquals(1, results.size());
        assertFalse(results.get(0).success());
    }

    // --- Stubs ---

    static class RecordingOrchestrator implements WorkerOrchestrator {
        private final String output;

        RecordingOrchestrator(String output) { this.output = output; }

        @Override
        public WorkerResult delegate(AgentDefinition worker, String task, AgentContext parentContext) {
            return new WorkerResult(worker.id(), output, true, List.of());
        }
    }

    static class FailingOrchestrator implements WorkerOrchestrator {
        @Override
        public WorkerResult delegate(AgentDefinition worker, String task, AgentContext parentContext) {
            throw new RuntimeException("orchestrator failed");
        }
    }
}
