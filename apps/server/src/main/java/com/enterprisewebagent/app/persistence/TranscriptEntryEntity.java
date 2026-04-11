package com.enterprisewebagent.app.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "transcript_entries")
public class TranscriptEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, insertable = false, updatable = false)
    private SessionEntity session;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(length = 20, nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sequence_num", nullable = false)
    private int sequenceNum;

    protected TranscriptEntryEntity() {}

    public TranscriptEntryEntity(String sessionId, String role, String content, Instant createdAt, int sequenceNum) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.sequenceNum = sequenceNum;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public SessionEntity getSession() { return session; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public int getSequenceNum() { return sequenceNum; }
}
