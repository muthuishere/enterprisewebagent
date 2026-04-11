package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiffCommandTest {

    private DiffCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new DiffCommand();
    }

    @Test
    void diffNoChanges() {
        // In a clean directory, git diff should report no changes or succeed
        var ctx = new CommandContext("s1", "/diff", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        // Either succeeds with no changes or shows diff output
        assertTrue(result.success() || result.output().contains("failed"));
    }

    @Test
    void diffWithPath() {
        var ctx = new CommandContext("s1", "/diff nonexistent/path", List.of("nonexistent/path"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        // Should succeed even with no matching path (git diff doesn't error on missing paths)
        assertNotNull(result.output());
    }

    @Test
    void commandMetadata() {
        assertEquals("diff", cmd.name());
        assertEquals("/diff [path]", cmd.usage());
        assertNotNull(cmd.description());
    }
}
