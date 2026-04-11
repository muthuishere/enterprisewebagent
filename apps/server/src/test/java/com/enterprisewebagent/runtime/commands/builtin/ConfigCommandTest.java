package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigCommandTest {

    private ConfigCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new ConfigCommand();
    }

    @Test
    void showEmptyConfig() {
        var ctx = new CommandContext("s1", "/config", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("No configuration set"));
    }

    @Test
    void setAndGetConfig() {
        var setCtx = new CommandContext("s1", "/config theme dark", List.of("theme", "dark"), null, null, null, Map.of());
        var setResult = cmd.execute(setCtx);
        assertTrue(setResult.success());
        assertTrue(setResult.output().contains("theme = dark"));

        var getCtx = new CommandContext("s1", "/config theme", List.of("theme"), null, null, null, Map.of());
        var getResult = cmd.execute(getCtx);
        assertTrue(getResult.success());
        assertEquals("theme = dark", getResult.output());
    }

    @Test
    void getUnsetKey() {
        var ctx = new CommandContext("s1", "/config nokey", List.of("nokey"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("not set"));
    }

    @Test
    void setMultiWordValue() {
        var ctx = new CommandContext("s1", "/config greeting hello world", List.of("greeting", "hello", "world"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("greeting = hello world"));
    }
}
