package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.tasks.TaskDefinition;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tasks.TaskStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskManager taskManager;

    public TaskController(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String description = body.get("description");
        TaskDefinition task = taskManager.create(sessionId, description);
        return ResponseEntity.ok(taskToMap(task));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        return taskManager.get(taskId)
                .map(t -> ResponseEntity.ok(taskToMap(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listTasks(@RequestParam String sessionId) {
        var tasks = taskManager.listBySession(sessionId).stream()
                .map(this::taskToMap)
                .toList();
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{taskId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String taskId,
                                              @RequestBody Map<String, String> body) {
        String status = body.get("status");
        taskManager.updateStatus(taskId, TaskStatus.valueOf(status));
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> taskToMap(TaskDefinition task) {
        return Map.of(
                "id", task.id(),
                "sessionId", task.sessionId(),
                "description", task.description(),
                "status", task.status().name(),
                "created", task.created().toString()
        );
    }
}
