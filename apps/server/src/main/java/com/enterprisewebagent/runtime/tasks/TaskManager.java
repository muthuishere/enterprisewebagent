package com.enterprisewebagent.runtime.tasks;

import java.util.List;
import java.util.Optional;

public interface TaskManager {
    TaskDefinition create(String sessionId, String description);
    void updateStatus(String taskId, TaskStatus status);
    Optional<TaskDefinition> get(String taskId);
    List<TaskDefinition> listBySession(String sessionId);
}
