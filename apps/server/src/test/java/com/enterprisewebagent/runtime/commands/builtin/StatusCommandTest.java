package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.StubModelProvider;
import com.enterprisewebagent.runtime.session.InMemorySessionManager;
import com.enterprisewebagent.runtime.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatusCommandTest {

    private StatusCommand cmd;
    private InMemorySessionManager sessionManager;
    private DefaultModelProviderRegistry providerRegistry;

    @BeforeEach
    void setUp() {
        cmd = new StatusCommand();
        sessionManager = new InMemorySessionManager();
        providerRegistry = new DefaultModelProviderRegistry();
        providerRegistry.register("stub", new StubModelProvider("hi"));
    }

    @Test
    void showsSessionStatus() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/status", List.of(), sessionManager, null, providerRegistry, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains(session.id()));
        assertTrue(result.output().contains("ACTIVE"));
        assertTrue(result.output().contains("stub"));
    }

    @Test
    void errorForUnknownSession() {
        var ctx = new CommandContext("nonexistent", "/status", List.of(), sessionManager, null, providerRegistry, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Session not found"));
    }

    @Test
    void showsWorkspaceId() {
        Session session = sessionManager.create("my-workspace");
        var ctx = new CommandContext(session.id(), "/status", List.of(), sessionManager, null, providerRegistry, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.output().contains("my-workspace"));
    }
}
