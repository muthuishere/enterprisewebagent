package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandRegistry;
import com.enterprisewebagent.runtime.commands.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HelpCommandTest {

    private CommandRegistry registry;
    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
        helpCommand = new HelpCommand(registry);
        registry.register(helpCommand);
        BuiltInCommandRegistrar.registerAll(registry);
    }

    @Test
    void listAllCommands() {
        var ctx = new CommandContext("s1", "/help", List.of(), null, null, null, Map.of());
        var result = helpCommand.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("Available commands:"));
        assertTrue(result.output().contains("/help"));
        assertTrue(result.output().contains("/status"));
    }

    @Test
    void helpForSpecificCommand() {
        var ctx = new CommandContext("s1", "/help status", List.of("status"), null, null, null, Map.of());
        var result = helpCommand.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("/status"));
        assertTrue(result.output().contains("Show session status"));
    }

    @Test
    void helpForUnknownCommand() {
        var ctx = new CommandContext("s1", "/help nonexistent", List.of("nonexistent"), null, null, null, Map.of());
        var result = helpCommand.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Unknown command"));
    }
}
