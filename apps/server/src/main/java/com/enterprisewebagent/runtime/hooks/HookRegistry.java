package com.enterprisewebagent.runtime.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);

    private final ConcurrentHashMap<String, Hook> hooks = new ConcurrentHashMap<>();
    private final HookExecutor hookExecutor;

    public HookRegistry() {
        this(new HookExecutor());
    }

    public HookRegistry(HookExecutor hookExecutor) {
        this.hookExecutor = hookExecutor;
    }

    public void register(Hook hook) {
        hooks.put(hook.id(), hook);
        log.debug("Registered hook: {} type={}", hook.id(), hook.type());
    }

    public void unregister(String hookId) {
        hooks.remove(hookId);
        log.debug("Unregistered hook: {}", hookId);
    }

    public List<Hook> getHooks(HookType type) {
        return hooks.values().stream()
                .filter(h -> h.type() == type)
                .filter(Hook::enabled)
                .sorted(Comparator.comparingInt(Hook::order))
                .toList();
    }

    public HookResult execute(HookType type, HookContext context) {
        List<Hook> typeHooks = getHooks(type);
        if (typeHooks.isEmpty()) {
            return HookResult.success("No hooks registered for " + type);
        }

        var outputs = new ArrayList<String>();

        for (Hook hook : typeHooks) {
            HookResult result = hookExecutor.execute(hook, context);
            if (!result.output().isBlank()) {
                outputs.add(hook.id() + ": " + result.output());
            }

            if (result.abort()) {
                log.info("Hook {} requested abort for {}", hook.id(), type);
                String combinedOutput = String.join("\n", outputs);
                return new HookResult(false, combinedOutput, true);
            }

            if (!result.success()) {
                log.warn("Hook {} failed for {}: {}", hook.id(), type, result.output());
            }
        }

        return HookResult.success(String.join("\n", outputs));
    }

    public List<Hook> getAllHooks() {
        return List.copyOf(hooks.values());
    }
}
