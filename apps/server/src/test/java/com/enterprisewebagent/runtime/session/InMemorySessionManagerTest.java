package com.enterprisewebagent.runtime.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySessionManagerTest {

    private InMemorySessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemorySessionManager();
    }

    @Test
    void createAndGetLifecycle() {
        var session = manager.create("workspace-1");

        assertNotNull(session.id());
        assertEquals("workspace-1", session.workspaceId());
        assertEquals(SessionStatus.ACTIVE, session.status());
        assertTrue(session.transcript().isEmpty());

        var retrieved = manager.get(session.id());
        assertTrue(retrieved.isPresent());
        assertEquals(session.id(), retrieved.get().id());
    }

    @Test
    void getNonExistentReturnsEmpty() {
        assertTrue(manager.get("missing").isEmpty());
    }

    @Test
    void resumeUpdatesStatusToActive() {
        var session = manager.create("ws-1");
        manager.close(session.id());

        var resumed = manager.resume(session.id());
        assertEquals(SessionStatus.ACTIVE, resumed.status());
        assertTrue(resumed.lastActive().isAfter(session.lastActive()) ||
                resumed.lastActive().equals(session.lastActive()));
    }

    @Test
    void closeUpdatesStatusToClosed() {
        var session = manager.create("ws-1");
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
}
