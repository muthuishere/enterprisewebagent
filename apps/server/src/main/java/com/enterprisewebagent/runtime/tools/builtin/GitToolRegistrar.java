package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;

public class GitToolRegistrar {

    public static void registerAll(DefaultToolRegistry registry) {
        registry.registerExecutor(new GitDiffTool());
        registry.registerExecutor(new GitCommitTool());
        registry.registerExecutor(new GitBranchTool());
        registry.registerExecutor(new GitLogTool());
        registry.registerExecutor(new GitStatusTool());
    }
}
