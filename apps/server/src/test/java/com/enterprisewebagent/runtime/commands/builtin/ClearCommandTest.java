package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.session.InMemorySessionManager;
import com.enterprisewebagent.runtime.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClearCommandTest {

    private ClearCommand cmd;
    private InMemorySessionManager sessionManager;

    @BeforeEach
    void setUp() {
        cmd = new ClearCommand();
        sessionManager = new InMemorySessionManager();
    }

    @Test
    void clearsSession() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/clear", List.of(), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("Conversation cleared"));
    }

    @Test
    void errorForUnknownSession() {
        var ctx = new CommandContext("nonexistent", "/clear", List.of(), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Session not found"));
    }

    @Test
    void newSessionIdReturnedAfterClear() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/clear", List.of(), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("New session:"));
    }
}
