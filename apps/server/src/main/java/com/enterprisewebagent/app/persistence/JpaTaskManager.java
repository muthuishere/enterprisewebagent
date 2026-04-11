package com.enterprisewebagent.app.persistence;

import com.enterprisewebagent.runtime.tasks.TaskDefinition;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tasks.TaskStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaTaskManager implements TaskManager {

    private final TaskRepository taskRepository;

    public JpaTaskManager(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public TaskDefinition create(String sessionId, String description) {
        var id = UUID.randomUUID().toString();
        var entity = new TaskEntity(id, sessionId, description, TaskStatus.PENDING, Instant.now());
        taskRepository.save(entity);
        return toRecord(entity);
    }

    @Override
    @Transactional
    public void updateStatus(String taskId, TaskStatus status) {
        var entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        entity.setStatus(status);
        taskRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskDefinition> get(String taskId) {
        return taskRepository.findById(taskId).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDefinition> listBySession(String sessionId) {
        return taskRepository.findBySessionId(sessionId).stream()
                .map(this::toRecord)
                .toList();
    }

    private TaskDefinition toRecord(TaskEntity entity) {
        return new TaskDefinition(entity.getId(), entity.getSessionId(), entity.getDescription(),
                entity.getStatus(), entity.getCreatedAt());
    }
}
