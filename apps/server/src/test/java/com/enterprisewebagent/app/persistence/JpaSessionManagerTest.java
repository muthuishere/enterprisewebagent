package com.enterprisewebagent.app.persistence;

import com.enterprisewebagent.runtime.query.TranscriptEntry;
import com.enterprisewebagent.runtime.session.Session;
import com.enterprisewebagent.runtime.session.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class JpaSessionManagerTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TranscriptEntryRepository transcriptEntryRepository;

    private JpaSessionManager manager;

    @BeforeEach
    void setUp() {
        transcriptEntryRepository.deleteAll();
        sessionRepository.deleteAll();
        manager = new JpaSessionManager(sessionRepository, transcriptEntryRepository);
    }

    @Test
    void createAndGetLifecycle() {
        Session session = manager.create("workspace-1");

        assertNotNull(session.id());
        assertEquals("workspace-1", session.workspaceId());
        assertEquals(SessionStatus.ACTIVE, session.status());
        assertTrue(session.transcript().isEmpty());

        var retrieved = manager.get(session.id());
        assertTrue(retrieved.isPresent());
        assertEquals(session.id(), retrieved.get().id());
        assertEquals("workspace-1", retrieved.get().workspaceId());
    }

    @Test
    void getNonExistentReturnsEmpty() {
        assertTrue(manager.get("missing").isEmpty());
    }

    @Test
    void resumeUpdatesStatusToActive() {
        Session session = manager.create("ws-1");
        manager.close(session.id());

        Session resumed = manager.resume(session.id());
        assertEquals(SessionStatus.ACTIVE, resumed.status());
        assertTrue(resumed.lastActive().isAfter(session.lastActive()) ||
                resumed.lastActive().equals(session.lastActive()));
    }

    @Test
    void closeUpdatesStatusToClosed() {
        Session session = manager.create("ws-1");
        manager.close(session.id());

        var closed = manager.get(session.id());
        assertTrue(closed.isPresent());
        assertEquals(SessionStatus.CLOSED, closed.get().status());
    }

    @Test
    void resumeNonExistentThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.resume("missing"));
    }

    @Test
    void closeNonExistentThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.close("missing"));
    }

    @Test
    void appendTranscriptPersistsEntries() {
        Session session = manager.create("ws-1");

        manager.appendTranscript(session.id(),
                new TranscriptEntry("user", "Hello", Instant.now()));
        manager.appendTranscript(session.id(),
                new TranscriptEntry("assistant", "Hi there!", Instant.now()));

        var retrieved = manager.get(session.id());
        assertTrue(retrieved.isPresent());
        assertEquals(2, retrieved.get().transcript().size());
        assertEquals("user", retrieved.get().transcript().get(0).role());
        assertEquals("Hello", retrieved.get().transcript().get(0).content());
        assertEquals("assistant", retrieved.get().transcript().get(1).role());
        assertEquals("Hi there!", retrieved.get().transcript().get(1).content());
    }

    @Test
    void appendTranscriptToNonExistentSessionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.appendTranscript("missing",
                        new TranscriptEntry("user", "Hello", Instant.now())));
    }

    @Test
    void appendTranscriptPreservesOrder() {
        Session session = manager.create("ws-1");

        for (int i = 0; i < 5; i++) {
            manager.appendTranscript(session.id(),
                    new TranscriptEntry("user", "Message " + i, Instant.now()));
        }

        var retrieved = manager.get(session.id());
        assertTrue(retrieved.isPresent());
        var transcript = retrieved.get().transcript();
        assertEquals(5, transcript.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("Message " + i, transcript.get(i).content());
        }
    }
}
