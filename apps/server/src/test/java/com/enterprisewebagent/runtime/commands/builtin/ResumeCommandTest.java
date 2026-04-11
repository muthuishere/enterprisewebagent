package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.session.InMemorySessionManager;
import com.enterprisewebagent.runtime.session.Session;
import com.enterprisewebagent.runtime.session.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResumeCommandTest {

    private ResumeCommand cmd;
    private InMemorySessionManager sessionManager;

    @BeforeEach
    void setUp() {
        cmd = new ResumeCommand();
        sessionManager = new InMemorySessionManager();
    }

    @Test
    void resumesExistingSession() {
        Session session = sessionManager.create("ws-1");
        sessionManager.close(session.id());
        var ctx = new CommandContext("current", "/resume " + session.id(), List.of(session.id()), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("Resumed session"));
        assertEquals(SessionStatus.ACTIVE, sessionManager.get(session.id()).get().status());
    }

    @Test
    void errorWithoutSessionId() {
        var ctx = new CommandContext("current", "/resume", List.of(), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Usage"));
    }

    @Test
    void errorForNonexistentSession() {
        var ctx = new CommandContext("current", "/resume bad-id", List.of("bad-id"), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Cannot resume"));
    }
}
