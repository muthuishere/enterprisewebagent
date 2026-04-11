package com.enterprisewebagent.runtime.session;

import com.enterprisewebagent.runtime.query.TranscriptEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionManager implements SessionManager {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session create(String workspaceId) {
        var id = UUID.randomUUID().toString();
        var now = Instant.now();
        var session = new Session(id, workspaceId, now, now, SessionStatus.ACTIVE, List.of());
        sessions.put(id, session);
        return session;
    }

    @Override
    public Optional<Session> get(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Session resume(String sessionId) {
        var existing = sessions.get(sessionId);
        if (existing == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        var resumed = new Session(
                existing.id(),
                existing.workspaceId(),
                existing.created(),
                Instant.now(),
                SessionStatus.ACTIVE,
                existing.transcript()
        );
        sessions.put(sessionId, resumed);
        return resumed;
    }

    @Override
    public void close(String sessionId) {
        var existing = sessions.get(sessionId);
        if (existing == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        var closed = new Session(
                existing.id(),
                existing.workspaceId(),
                existing.created(),
                Instant.now(),
                SessionStatus.CLOSED,
                existing.transcript()
        );
        sessions.put(sessionId, closed);
    }

    @Override
    public void appendTranscript(String sessionId, TranscriptEntry entry) {
        var existing = sessions.get(sessionId);
        if (existing == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        var newTranscript = new ArrayList<>(existing.transcript());
        newTranscript.add(entry);
        var updated = new Session(
                existing.id(),
                existing.workspaceId(),
                existing.created(),
                Instant.now(),
                existing.status(),
                List.copyOf(newTranscript)
        );
        sessions.put(sessionId, updated);
    }
}
