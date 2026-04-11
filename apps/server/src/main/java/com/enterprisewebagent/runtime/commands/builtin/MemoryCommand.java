package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCommand implements Command {

    private final Map<String, java.util.List<String>> sessionMemories = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public String description() {
        return "View/manage session memory";
    }

    @Override
    public String usage() {
        return "/memory [show|add <text>|clear]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String subcommand = context.args().isEmpty() ? "show" : context.args().getFirst();

        return switch (subcommand) {
            case "show" -> {
                var memories = sessionMemories.getOrDefault(context.sessionId(), java.util.List.of());
                if (memories.isEmpty()) {
                    yield CommandResult.success("No memory entries for this session.");
                }
                var sb = new StringBuilder("Session Memory (" + memories.size() + " entries):\n");
                for (int i = 0; i < memories.size(); i++) {
                    sb.append("  %d. %s%n".formatted(i + 1, memories.get(i)));
                }
                yield CommandResult.success(sb.toString());
            }
            case "add" -> {
                if (context.args().size() < 2) {
                    yield CommandResult.error("Usage: /memory add <text>");
                }
                String text = String.join(" ", context.args().subList(1, context.args().size()));
                sessionMemories.computeIfAbsent(context.sessionId(), k -> new java.util.ArrayList<>()).add(text);
                yield CommandResult.success("Memory added: " + text);
            }
            case "clear" -> {
                sessionMemories.remove(context.sessionId());
                yield CommandResult.success("Session memory cleared.");
            }
            default -> CommandResult.error("Unknown subcommand: " + subcommand + ". Use show, add, or clear.");
        };
    }
}
