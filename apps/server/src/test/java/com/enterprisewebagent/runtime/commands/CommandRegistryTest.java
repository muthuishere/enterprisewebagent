package com.enterprisewebagent.runtime.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
    }

    @Test
    void register_and_lookup() {
        Command cmd = stubCommand("test", "A test command");
        registry.register(cmd);
        assertTrue(registry.get("test").isPresent());
        assertEquals("test", registry.get("test").get().name());
    }

    @Test
    void get_returnsEmptyForUnknown() {
        assertTrue(registry.get("nonexistent").isEmpty());
    }

    @Test
    void listAll_returnsSortedCommands() {
        registry.register(stubCommand("zebra", "Z command"));
        registry.register(stubCommand("alpha", "A command"));
        registry.register(stubCommand("middle", "M command"));

        var all = registry.listAll();
        assertEquals(3, all.size());
        assertEquals("alpha", all.get(0).name());
        assertEquals("middle", all.get(1).name());
        assertEquals("zebra", all.get(2).name());
    }

    @Test
    void size_reflectsRegisteredCount() {
        assertEquals(0, registry.size());
        registry.register(stubCommand("a", "A"));
        assertEquals(1, registry.size());
        registry.register(stubCommand("b", "B"));
        assertEquals(2, registry.size());
    }

    @Test
    void register_overwritesSameName() {
        registry.register(stubCommand("dup", "first"));
        registry.register(stubCommand("dup", "second"));
        assertEquals(1, registry.size());
        assertEquals("second", registry.get("dup").get().description());
    }

    private Command stubCommand(String name, String description) {
        return new Command() {
            @Override public String name() { return name; }
            @Override public String description() { return description; }
            @Override public String usage() { return "/" + name; }
            @Override public CommandResult execute(CommandContext context) { return CommandResult.success("ok"); }
        };
    }
}
