package com.enterprisewebagent.app.persistence;

import com.enterprisewebagent.runtime.tasks.TaskStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskEntity() {}

    public TaskEntity(String id, String sessionId, String description, TaskStatus status, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
