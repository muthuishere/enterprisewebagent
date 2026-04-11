package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;

public class BuiltInToolRegistrar {

    public static void registerAll(DefaultToolRegistry registry) {
        registry.registerExecutor(new AskUserTool());
        registry.registerExecutor(new FileReadTool());
        registry.registerExecutor(new FileEditTool());
        registry.registerExecutor(new ShellTool());
        registry.registerExecutor(new TaskStopTool());
        registry.registerExecutor(new WorkerDelegationTool());
    }
}
