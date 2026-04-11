package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

public class ModelCommand implements Command {

    @Override
    public String name() {
        return "model";
    }

    @Override
    public String description() {
        return "Show/switch current model";
    }

    @Override
    public String usage() {
        return "/model [name]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        var registry = context.modelProviderRegistry();
        if (registry == null) {
            return CommandResult.error("Model provider registry not available.");
        }

        if (context.args().isEmpty()) {
            var sb = new StringBuilder("Model Configuration\n");
            sb.append("  Current: ").append(registry.getDefaultProviderId()).append("\n");
            sb.append("  Available: ").append(String.join(", ", registry.availableProviders()));
            return CommandResult.success(sb.toString());
        }

        String requested = context.args().getFirst();
        try {
            registry.setDefault(requested);
            return CommandResult.success("Switched to model provider: " + requested);
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Unknown provider: " + requested
                    + ". Available: " + String.join(", ", registry.availableProviders()));
        }
    }
}
