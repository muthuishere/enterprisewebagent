package com.enterprisewebagent.app.persistence;

import com.enterprisewebagent.runtime.session.SessionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SessionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNum ASC")
    private List<TranscriptEntryEntity> transcriptEntries = new ArrayList<>();

    protected SessionEntity() {}

    public SessionEntity(String id, String workspaceId, SessionStatus status, Instant createdAt, Instant lastActiveAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.status = status;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    public List<TranscriptEntryEntity> getTranscriptEntries() { return transcriptEntries; }
}
