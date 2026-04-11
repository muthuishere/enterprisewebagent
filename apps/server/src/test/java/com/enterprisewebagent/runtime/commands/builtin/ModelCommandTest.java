package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.StubModelProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelCommandTest {

    private ModelCommand cmd;
    private DefaultModelProviderRegistry registry;

    @BeforeEach
    void setUp() {
        cmd = new ModelCommand();
        registry = new DefaultModelProviderRegistry();
        registry.register("stub", new StubModelProvider("hi"));
        registry.register("openai", new StubModelProvider("hello"));
    }

    @Test
    void showCurrentModel() {
        var ctx = new CommandContext("s1", "/model", List.of(), null, null, registry, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("stub"));
        assertTrue(result.output().contains("openai"));
    }

    @Test
    void switchModel() {
        var ctx = new CommandContext("s1", "/model openai", List.of("openai"), null, null, registry, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("openai"));
        assertEquals("openai", registry.getDefaultProviderId());
    }

    @Test
    void switchToUnknownModel() {
        var ctx = new CommandContext("s1", "/model nonexistent", List.of("nonexistent"), null, null, registry, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
        assertTrue(result.output().contains("Unknown provider"));
    }

    @Test
    void nullRegistry_returnsError() {
        var ctx = new CommandContext("s1", "/model", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
    }
}
