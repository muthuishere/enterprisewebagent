package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandRegistry;

public class BuiltInCommandRegistrar {

    public static void registerAll(CommandRegistry registry) {
        registry.register(new HelpCommand(registry));
        registry.register(new StatusCommand());
        registry.register(new ClearCommand());
        registry.register(new ResumeCommand());
        registry.register(new CostCommand());
        registry.register(new MemoryCommand());
        registry.register(new SkillsCommand());
        registry.register(new ConfigCommand());
        registry.register(new PlanCommand());
        registry.register(new ReviewCommand());
        registry.register(new DiffCommand());
        registry.register(new CommitCommand());
        registry.register(new VersionCommand());
        registry.register(new SessionCommand());
        registry.register(new ModelCommand());
    }
}
