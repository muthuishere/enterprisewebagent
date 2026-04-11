package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanCommandTest {

    private PlanCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new PlanCommand();
    }

    @Test
    void showDefault() {
        var ctx = new CommandContext("s1", "/plan", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("OFF"));
    }

    @Test
    void enablePlanMode() {
        var ctx = new CommandContext("s1", "/plan on", List.of("on"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("enabled"));
        assertTrue(cmd.isPlanMode("s1"));
    }

    @Test
    void disablePlanMode() {
        cmd.execute(new CommandContext("s1", "/plan on", List.of("on"), null, null, null, Map.of()));
        var ctx = new CommandContext("s1", "/plan off", List.of("off"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("disabled"));
        assertFalse(cmd.isPlanMode("s1"));
    }

    @Test
    void invalidToggle() {
        var ctx = new CommandContext("s1", "/plan maybe", List.of("maybe"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
    }
}
