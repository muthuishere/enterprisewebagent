package com.enterprisewebagent.runtime.hooks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HookRegistryTest {

    private HookRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new HookRegistry();
    }

    @Test
    void registerAndRetrieveHooks() {
        var hook = new Hook("h1", HookType.PRE_TURN, "echo pre-turn", 1, true);
        registry.register(hook);

        var hooks = registry.getHooks(HookType.PRE_TURN);
        assertEquals(1, hooks.size());
        assertEquals("h1", hooks.get(0).id());
    }

    @Test
    void getHooksReturnsEmptyForUnregisteredType() {
        var hooks = registry.getHooks(HookType.POST_TURN);
        assertTrue(hooks.isEmpty());
    }

    @Test
    void unregisterRemovesHook() {
        var hook = new Hook("h1", HookType.PRE_TURN, "echo test", 1, true);
        registry.register(hook);
        registry.unregister("h1");

        var hooks = registry.getHooks(HookType.PRE_TURN);
        assertTrue(hooks.isEmpty());
    }

    @Test
    void disabledHooksAreFiltered() {
        var hook = new Hook("h1", HookType.PRE_TURN, "echo test", 1, false);
        registry.register(hook);

        var hooks = registry.getHooks(HookType.PRE_TURN);
        assertTrue(hooks.isEmpty());
    }

    @Test
    void hooksAreReturnedInOrder() {
        registry.register(new Hook("h3", HookType.PRE_TURN, "echo 3", 3, true));
        registry.register(new Hook("h1", HookType.PRE_TURN, "echo 1", 1, true));
        registry.register(new Hook("h2", HookType.PRE_TURN, "echo 2", 2, true));

        var hooks = registry.getHooks(HookType.PRE_TURN);
        assertEquals(3, hooks.size());
        assertEquals("h1", hooks.get(0).id());
        assertEquals("h2", hooks.get(1).id());
        assertEquals("h3", hooks.get(2).id());
    }

    @Test
    void executeRunsHooksInOrder() {
        registry.register(new Hook("h1", HookType.PRE_TURN, "echo first", 1, true));
        registry.register(new Hook("h2", HookType.PRE_TURN, "echo second", 2, true));

        var context = new HookContext("sess-1", HookType.PRE_TURN, Map.of());
        HookResult result = registry.execute(HookType.PRE_TURN, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("first"));
        assertTrue(result.output().contains("second"));
    }

    @Test
    void executeReturnsSuccessForNoHooks() {
        var context = new HookContext("sess-1", HookType.POST_TURN, Map.of());
        HookResult result = registry.execute(HookType.POST_TURN, context);
        assertTrue(result.success());
    }

    @Test
    void executeStopsOnAbort() {
        // exit code 2 = abort
        registry.register(new Hook("h1", HookType.PRE_TURN, "exit 2", 1, true));
        registry.register(new Hook("h2", HookType.PRE_TURN, "echo should-not-run", 2, true));

        var context = new HookContext("sess-1", HookType.PRE_TURN, Map.of());
        HookResult result = registry.execute(HookType.PRE_TURN, context);

        assertTrue(result.abort());
        assertFalse(result.success());
    }

    @Test
    void getAllHooksReturnsAll() {
        registry.register(new Hook("h1", HookType.PRE_TURN, "echo 1", 1, true));
        registry.register(new Hook("h2", HookType.POST_TURN, "echo 2", 1, true));
        registry.register(new Hook("h3", HookType.PRE_TOOL, "echo 3", 1, false));

        assertEquals(3, registry.getAllHooks().size());
    }
}
