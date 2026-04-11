package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigCommand implements Command {

    private final Map<String, Map<String, String>> sessionConfigs = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "config";
    }

    @Override
    public String description() {
        return "View/set configuration";
    }

    @Override
    public String usage() {
        return "/config [key] [value]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        var config = sessionConfigs.computeIfAbsent(context.sessionId(), k -> new ConcurrentHashMap<>());

        if (context.args().isEmpty()) {
            if (config.isEmpty()) {
                return CommandResult.success("No configuration set for this session.");
            }
            var sb = new StringBuilder("Configuration:\n");
            config.forEach((k, v) -> sb.append("  %s = %s%n".formatted(k, v)));
            return CommandResult.success(sb.toString());
        }

        String key = context.args().getFirst();

        if (context.args().size() == 1) {
            String value = config.get(key);
            if (value == null) {
                return CommandResult.error("Config key not set: " + key);
            }
            return CommandResult.success(key + " = " + value);
        }

        String value = String.join(" ", context.args().subList(1, context.args().size()));
        config.put(key, value);
        return CommandResult.success("Set " + key + " = " + value);
    }
}
