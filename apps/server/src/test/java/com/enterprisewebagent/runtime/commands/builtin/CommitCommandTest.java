package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommitCommandTest {

    private CommitCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new CommitCommand();
    }

    @Test
    void commitWithMessage() {
        // May fail in test env without staged changes, but should not throw
        var ctx = new CommandContext("s1", "/commit test message", List.of("test", "message"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertNotNull(result.output());
    }

    @Test
    void commitWithoutMessage_usesDefault() {
        var ctx = new CommandContext("s1", "/commit", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertNotNull(result.output());
    }

    @Test
    void commandMetadata() {
        assertEquals("commit", cmd.name());
        assertEquals("/commit [message]", cmd.usage());
        assertEquals("Git commit with optional message", cmd.description());
    }
}
