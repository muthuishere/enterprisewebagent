package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MemoryCommandTest {

    private MemoryCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new MemoryCommand();
    }

    @Test
    void showEmpty() {
        var ctx = new CommandContext("s1", "/memory", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("No memory entries"));
    }

    @Test
    void addAndShow() {
        var addCtx = new CommandContext("s1", "/memory add remember this", List.of("add", "remember", "this"), null, null, null, Map.of());
        var addResult = cmd.execute(addCtx);
        assertTrue(addResult.success());
        assertTrue(addResult.output().contains("Memory added"));

        var showCtx = new CommandContext("s1", "/memory show", List.of("show"), null, null, null, Map.of());
        var showResult = cmd.execute(showCtx);
        assertTrue(showResult.output().contains("remember this"));
    }

    @Test
    void clearMemory() {
        var addCtx = new CommandContext("s1", "/memory add data", List.of("add", "data"), null, null, null, Map.of());
        cmd.execute(addCtx);

        var clearCtx = new CommandContext("s1", "/memory clear", List.of("clear"), null, null, null, Map.of());
        var result = cmd.execute(clearCtx);
        assertTrue(result.success());
        assertTrue(result.output().contains("cleared"));

        var showCtx = new CommandContext("s1", "/memory show", List.of("show"), null, null, null, Map.of());
        assertTrue(cmd.execute(showCtx).output().contains("No memory entries"));
    }

    @Test
    void addWithoutText_returnsError() {
        var ctx = new CommandContext("s1", "/memory add", List.of("add"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
    }

    @Test
    void unknownSubcommand() {
        var ctx = new CommandContext("s1", "/memory foo", List.of("foo"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Unknown subcommand"));
    }
}
