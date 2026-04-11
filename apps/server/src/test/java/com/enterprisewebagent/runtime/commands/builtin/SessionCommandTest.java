package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.session.InMemorySessionManager;
import com.enterprisewebagent.runtime.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SessionCommandTest {

    private SessionCommand cmd;
    private InMemorySessionManager sessionManager;

    @BeforeEach
    void setUp() {
        cmd = new SessionCommand();
        sessionManager = new InMemorySessionManager();
    }

    @Test
    void listCurrentSession() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/session list", List.of("list"), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains(session.id()));
        assertTrue(result.output().contains("ACTIVE"));
    }

    @Test
    void tagSession() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/session tag my-tag", List.of("tag", "my-tag"), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("my-tag"));
    }

    @Test
    void exportSession() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/session export", List.of("export"), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("id"));
        assertTrue(result.output().contains(session.id()));
    }

    @Test
    void tagWithoutLabel_returnsError() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/session tag", List.of("tag"), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
    }

    @Test
    void unknownSubcommand() {
        Session session = sessionManager.create("ws-1");
        var ctx = new CommandContext(session.id(), "/session foo", List.of("foo"), sessionManager, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
    }
}
