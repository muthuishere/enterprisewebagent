package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCommand implements Command {

    private final Map<String, Map<String, String>> sessionTags = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "session";
    }

    @Override
    public String description() {
        return "Session management";
    }

    @Override
    public String usage() {
        return "/session [list|tag <label>|export]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String subcommand = context.args().isEmpty() ? "list" : context.args().getFirst();

        return switch (subcommand) {
            case "list" -> {
                var sessionOpt = context.sessionManager().get(context.sessionId());
                if (sessionOpt.isEmpty()) {
                    yield CommandResult.error("Session not found.");
                }
                var session = sessionOpt.get();
                var tags = sessionTags.getOrDefault(context.sessionId(), Map.of());
                var sb = new StringBuilder("Current session:\n");
                sb.append("  ID:        ").append(session.id()).append("\n");
                sb.append("  Status:    ").append(session.status()).append("\n");
                sb.append("  Created:   ").append(session.created()).append("\n");
                if (!tags.isEmpty()) {
                    sb.append("  Tags:      ");
                    tags.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
                    sb.append("\n");
                }
                sb.append("  Transcript: ").append(session.transcript().size()).append(" entries");
                yield CommandResult.success(sb.toString());
            }
            case "tag" -> {
                if (context.args().size() < 2) {
                    yield CommandResult.error("Usage: /session tag <label>");
                }
                String label = context.args().get(1);
                sessionTags.computeIfAbsent(context.sessionId(), k -> new ConcurrentHashMap<>())
                        .put("label", label);
                yield CommandResult.success("Session tagged: " + label);
            }
            case "export" -> {
                var sessionOpt = context.sessionManager().get(context.sessionId());
                if (sessionOpt.isEmpty()) {
                    yield CommandResult.error("Session not found.");
                }
                var session = sessionOpt.get();
                var sb = new StringBuilder("Session Export (JSON placeholder):\n");
                sb.append("{\n");
                sb.append("  \"id\": \"").append(session.id()).append("\",\n");
                sb.append("  \"workspace\": \"").append(session.workspaceId()).append("\",\n");
                sb.append("  \"status\": \"").append(session.status()).append("\",\n");
                sb.append("  \"transcriptEntries\": ").append(session.transcript().size()).append("\n");
                sb.append("}");
                yield CommandResult.success(sb.toString());
            }
            default -> CommandResult.error("Unknown subcommand: " + subcommand + ". Use list, tag, or export.");
        };
    }
}
