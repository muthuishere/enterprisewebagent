package com.enterprisewebagent.runtime.tasks;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTaskManager implements TaskManager {

    private final ConcurrentHashMap<String, TaskDefinition> tasks = new ConcurrentHashMap<>();

    @Override
    public TaskDefinition create(String sessionId, String description) {
        var id = UUID.randomUUID().toString();
        var task = new TaskDefinition(id, sessionId, description, TaskStatus.PENDING, Instant.now());
        tasks.put(id, task);
        return task;
    }

    @Override
    public void updateStatus(String taskId, TaskStatus status) {
        var existing = tasks.get(taskId);
        if (existing == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        var updated = new TaskDefinition(
                existing.id(),
                existing.sessionId(),
                existing.description(),
                status,
                existing.created()
        );
        tasks.put(taskId, updated);
    }

    @Override
    public Optional<TaskDefinition> get(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public List<TaskDefinition> listBySession(String sessionId) {
        return tasks.values().stream()
                .filter(t -> sessionId.equals(t.sessionId()))
                .toList();
    }
}
