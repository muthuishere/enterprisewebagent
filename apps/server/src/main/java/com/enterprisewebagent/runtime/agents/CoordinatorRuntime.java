package com.enterprisewebagent.runtime.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CoordinatorRuntime {

    private final WorkerOrchestrator orchestrator;
    private final AgentContext context;
    private final List<WorkerResult> completedWorkers = new ArrayList<>();

    public CoordinatorRuntime(WorkerOrchestrator orchestrator, String sessionId) {
        this.orchestrator = orchestrator;
        this.context = new AgentContext(sessionId, AgentRole.COORDINATOR, Map.of());
    }

    public WorkerResult runWorker(AgentDefinition worker, String task) {
        WorkerResult result = orchestrator.delegate(worker, task, context);
        synchronized (completedWorkers) {
            completedWorkers.add(result);
        }
        return result;
    }

    public List<WorkerResult> runParallel(List<Map.Entry<AgentDefinition, String>> workerTasks) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<WorkerResult>> futures = workerTasks.stream()
                .map(entry -> executor.submit(() ->
                    orchestrator.delegate(entry.getKey(), entry.getValue(), context)))
                .toList();

            List<WorkerResult> results = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return new WorkerResult("unknown", e.getMessage(), false, List.of());
                    }
                })
                .toList();

            synchronized (completedWorkers) {
                completedWorkers.addAll(results);
            }
            return results;
        }
    }

    public List<WorkerResult> completedWorkers() {
        synchronized (completedWorkers) {
            return Collections.unmodifiableList(new ArrayList<>(completedWorkers));
        }
    }
}
