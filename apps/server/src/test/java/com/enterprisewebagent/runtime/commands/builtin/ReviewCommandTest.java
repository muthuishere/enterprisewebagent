package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCommandTest {

    private ReviewCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new ReviewCommand();
    }

    @Test
    void reviewWithPath() {
        var ctx = new CommandContext("s1", "/review src/main", List.of("src/main"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertFalse(result.suppressTurn()); // pass-through to model
        assertTrue(result.output().contains("src/main"));
    }

    @Test
    void reviewWithoutPath_usesCurrentDir() {
        var ctx = new CommandContext("s1", "/review", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertFalse(result.suppressTurn());
        assertTrue(result.output().contains("."));
    }

    @Test
    void reviewOutputContainsInstruction() {
        var ctx = new CommandContext("s1", "/review myfile.java", List.of("myfile.java"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.output().contains("review"));
        assertTrue(result.output().contains("myfile.java"));
    }
}
