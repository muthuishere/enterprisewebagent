package com.enterprisewebagent.app.persistence;

import com.enterprisewebagent.runtime.query.TranscriptEntry;
import com.enterprisewebagent.runtime.session.Session;
import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.session.SessionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaSessionManager implements SessionManager {

    private final SessionRepository sessionRepository;
    private final TranscriptEntryRepository transcriptEntryRepository;

    public JpaSessionManager(SessionRepository sessionRepository, TranscriptEntryRepository transcriptEntryRepository) {
        this.sessionRepository = sessionRepository;
        this.transcriptEntryRepository = transcriptEntryRepository;
    }

    @Override
    @Transactional
    public Session create(String workspaceId) {
        var id = UUID.randomUUID().toString();
        var now = Instant.now();
        var entity = new SessionEntity(id, workspaceId, SessionStatus.ACTIVE, now, now);
        sessionRepository.save(entity);
        return toRecord(entity, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> get(String sessionId) {
        return sessionRepository.findById(sessionId)
                .map(entity -> {
                    var entries = transcriptEntryRepository.findBySessionIdOrderBySequenceNum(sessionId);
                    return toRecord(entity, entries);
                });
    }

    @Override
    @Transactional
    public Session resume(String sessionId) {
        var entity = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        entity.setStatus(SessionStatus.ACTIVE);
        entity.setLastActiveAt(Instant.now());
        sessionRepository.save(entity);
        var entries = transcriptEntryRepository.findBySessionIdOrderBySequenceNum(sessionId);
        return toRecord(entity, entries);
    }

    @Override
    @Transactional
    public void close(String sessionId) {
        var entity = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        entity.setStatus(SessionStatus.CLOSED);
        entity.setLastActiveAt(Instant.now());
        sessionRepository.save(entity);
    }

    @Transactional
    public void appendTranscript(String sessionId, TranscriptEntry entry) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        int nextSeq = transcriptEntryRepository.countBySessionId(sessionId);
        var timestamp = entry.timestamp() != null ? entry.timestamp() : Instant.now();
        var entryEntity = new TranscriptEntryEntity(sessionId, entry.role(), entry.content(), timestamp, nextSeq);
        transcriptEntryRepository.save(entryEntity);
    }

    private Session toRecord(SessionEntity entity, List<TranscriptEntryEntity> entries) {
        var transcript = entries.stream()
                .map(e -> new TranscriptEntry(e.getRole(), e.getContent(), e.getCreatedAt()))
                .toList();
        return new Session(entity.getId(), entity.getWorkspaceId(), entity.getCreatedAt(),
                entity.getLastActiveAt(), entity.getStatus(), transcript);
    }

    @Override
    public void tagSession(String sessionId, String tag) {
        throw new UnsupportedOperationException("Not implemented in JPA session manager");
    }

    @Override
    public List<String> getTags(String sessionId) {
        throw new UnsupportedOperationException("Not implemented in JPA session manager");
    }

    @Override
    public List<Session> findByTag(String tag) {
        throw new UnsupportedOperationException("Not implemented in JPA session manager");
    }

    @Override
    public String exportSession(String sessionId, String format) {
        throw new UnsupportedOperationException("Not implemented in JPA session manager");
    }
}
