package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.events.RuntimeEventPublisher;
import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;

public class BuiltInToolRegistrar {

    public static void registerAll(DefaultToolRegistry registry, RuntimeEventPublisher eventPublisher) {
        registry.registerExecutor(new AskUserTool(eventPublisher));
        registry.registerExecutor(new FileReadTool());
        registry.registerExecutor(new FileEditTool());
        registry.registerExecutor(new ShellTool());
        registry.registerExecutor(new TaskStopTool());
        registry.registerExecutor(new WorkerDelegationTool());
        registry.registerExecutor(new GlobTool());
        registry.registerExecutor(new GrepTool());
        registry.registerExecutor(new WebSearchTool());
        registry.registerExecutor(new WebFetchTool());
        registry.registerExecutor(new FileWriteTool());
        registry.registerExecutor(new SleepTool());
    }
}
