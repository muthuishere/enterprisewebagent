package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlanCommand implements Command {

    private final Map<String, Boolean> planModes = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "plan";
    }

    @Override
    public String description() {
        return "Toggle plan mode";
    }

    @Override
    public String usage() {
        return "/plan [on|off]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.args().isEmpty()) {
            boolean current = planModes.getOrDefault(context.sessionId(), false);
            return CommandResult.success("Plan mode is " + (current ? "ON" : "OFF"));
        }

        String toggle = context.args().getFirst().toLowerCase();
        return switch (toggle) {
            case "on" -> {
                planModes.put(context.sessionId(), true);
                yield CommandResult.success("Plan mode enabled. The agent will propose plans before acting.");
            }
            case "off" -> {
                planModes.put(context.sessionId(), false);
                yield CommandResult.success("Plan mode disabled. The agent will act directly.");
            }
            default -> CommandResult.error("Usage: /plan [on|off]");
        };
    }

    public boolean isPlanMode(String sessionId) {
        return planModes.getOrDefault(sessionId, false);
    }
}
