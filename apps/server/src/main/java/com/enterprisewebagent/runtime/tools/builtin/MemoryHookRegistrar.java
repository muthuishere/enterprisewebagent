package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.memory.MemoryFileLoader;
import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;

public class MemoryHookRegistrar {

    public static void registerAll(DefaultToolRegistry registry, MemoryFileLoader memoryLoader) {
        registry.registerExecutor(new MemoryTool(memoryLoader));
    }
}
