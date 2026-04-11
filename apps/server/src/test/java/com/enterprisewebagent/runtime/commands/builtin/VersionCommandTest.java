package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VersionCommandTest {

    private VersionCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new VersionCommand();
    }

    @Test
    void showsVersion() {
        var ctx = new CommandContext("s1", "/version", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("Enterprise Web Agent"));
        assertTrue(result.output().contains("0.0.1-SNAPSHOT"));
    }

    @Test
    void showsJavaVersion() {
        var ctx = new CommandContext("s1", "/version", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.output().contains("Java:"));
    }

    @Test
    void commandMetadata() {
        assertEquals("version", cmd.name());
        assertEquals("/version", cmd.usage());
        assertNotNull(cmd.description());
    }
}
