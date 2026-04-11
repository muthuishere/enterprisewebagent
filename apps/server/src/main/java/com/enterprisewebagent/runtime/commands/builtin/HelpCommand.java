package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandRegistry;
import com.enterprisewebagent.runtime.commands.CommandResult;

public class HelpCommand implements Command {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "List all commands or get help for a specific command";
    }

    @Override
    public String usage() {
        return "/help [command]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (!context.args().isEmpty()) {
            String cmdName = context.args().getFirst();
            var cmd = registry.get(cmdName);
            if (cmd.isEmpty()) {
                return CommandResult.error("Unknown command: " + cmdName);
            }
            var c = cmd.get();
            return CommandResult.success(
                    "/%s — %s\nUsage: %s".formatted(c.name(), c.description(), c.usage()));
        }

        var sb = new StringBuilder("Available commands:\n");
        for (var cmd : registry.listAll()) {
            sb.append("  /%-12s %s%n".formatted(cmd.name(), cmd.description()));
        }
        sb.append("\nType /help <command> for details.");
        return CommandResult.success(sb.toString());
    }
}
