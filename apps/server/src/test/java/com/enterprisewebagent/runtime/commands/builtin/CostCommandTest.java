package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.session.InMemorySessionManager;
import com.enterprisewebagent.runtime.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CostCommandTest {

    private CostCommand cmd;
    private InMemorySessionManager sessionManager;

    @BeforeEach
    void setUp() {
        cmd = new CostCommand();
        sessionManager = new InMemorySessionManager();
    }

    @Test
    void showsCostSummary() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/cost", List.of(), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("Cost Summary"));
        assertTrue(result.output().contains("Turns:"));
    }

    @Test
    void errorForUnknownSession() {
        var ctx = new CommandContext("nonexistent", "/cost", List.of(), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Session not found"));
    }

    @Test
    void showsTokenPlaceholder() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/cost", List.of(), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.output().contains("not yet implemented"));
    }
}
