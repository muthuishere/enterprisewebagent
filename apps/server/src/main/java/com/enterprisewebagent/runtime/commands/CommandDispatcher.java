package com.enterprisewebagent.runtime.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandDispatcher {

    private final CommandRegistry registry;

    public CommandDispatcher(CommandRegistry registry) {
        this.registry = registry;
    }

    public boolean isCommand(String input) {
        return input != null && input.trim().startsWith("/");
    }

    public CommandResult dispatch(String input, CommandContext context) {
        String trimmed = input.trim();
        String[] parts = trimmed.split("\\s+");
        String name = parts[0].substring(1); // strip leading "/"

        List<String> args = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            args.add(parts[i]);
        }

        var cmdOpt = registry.get(name);
        if (cmdOpt.isEmpty()) {
            return CommandResult.error("Unknown command: /" + name + ". Type /help for available commands.");
        }

        var ctx = new CommandContext(
                context.sessionId(),
                context.rawInput(),
                args,
                context.sessionManager(),
                context.taskManager(),
                context.modelProviderRegistry(),
                context.extra()
        );

        try {
            return cmdOpt.get().execute(ctx);
        } catch (Exception e) {
            return CommandResult.error("Command /" + name + " failed: " + e.getMessage());
        }
    }

    public CommandRegistry getRegistry() {
        return registry;
    }
}
