package com.enterprisewebagent.runtime.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandDispatcherTest {

    private CommandRegistry registry;
    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
        dispatcher = new CommandDispatcher(registry);

        registry.register(new Command() {
            @Override public String name() { return "echo"; }
            @Override public String description() { return "Echo args"; }
            @Override public String usage() { return "/echo <text>"; }
            @Override public CommandResult execute(CommandContext context) {
                return CommandResult.success("echo: " + String.join(" ", context.args()));
            }
        });
    }

    @Test
    void isCommand_trueForSlashPrefix() {
        assertTrue(dispatcher.isCommand("/help"));
        assertTrue(dispatcher.isCommand("  /status"));
        assertTrue(dispatcher.isCommand("/echo arg1 arg2"));
    }

    @Test
    void isCommand_falseForNonCommand() {
        assertFalse(dispatcher.isCommand("hello"));
        assertFalse(dispatcher.isCommand(""));
        assertFalse(dispatcher.isCommand(null));
    }

    @Test
    void dispatch_parsesNameAndArgs() {
        var ctx = new CommandContext("s1", "/echo hello world", List.of(), null, null, null, Map.of());
        var result = dispatcher.dispatch("/echo hello world", ctx);
        assertTrue(result.success());
        assertEquals("echo: hello world", result.output());
    }

    @Test
    void dispatch_unknownCommand() {
        var ctx = new CommandContext("s1", "/unknown", List.of(), null, null, null, Map.of());
        var result = dispatcher.dispatch("/unknown", ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Unknown command"));
    }

    @Test
    void dispatch_handlesExceptionInCommand() {
        registry.register(new Command() {
            @Override public String name() { return "boom"; }
            @Override public String description() { return "Throws"; }
            @Override public String usage() { return "/boom"; }
            @Override public CommandResult execute(CommandContext context) {
                throw new RuntimeException("kaboom");
            }
        });

        var ctx = new CommandContext("s1", "/boom", List.of(), null, null, null, Map.of());
        var result = dispatcher.dispatch("/boom", ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("kaboom"));
    }

    @Test
    void dispatch_noArgsCommand() {
        registry.register(new Command() {
            @Override public String name() { return "noargs"; }
            @Override public String description() { return "No args"; }
            @Override public String usage() { return "/noargs"; }
            @Override public CommandResult execute(CommandContext context) {
                return CommandResult.success("args=" + context.args().size());
            }
        });

        var ctx = new CommandContext("s1", "/noargs", List.of(), null, null, null, Map.of());
        var result = dispatcher.dispatch("/noargs", ctx);
        assertEquals("args=0", result.output());
    }
}
