package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

public class StatusCommand implements Command {

    @Override
    public String name() {
        return "status";
    }

    @Override
    public String description() {
        return "Show session status";
    }

    @Override
    public String usage() {
        return "/status";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        var sessionOpt = context.sessionManager().get(context.sessionId());
        if (sessionOpt.isEmpty()) {
            return CommandResult.error("Session not found: " + context.sessionId());
        }
        var session = sessionOpt.get();
        int turns = session.transcript().size();
        String provider = context.modelProviderRegistry() != null
                ? context.modelProviderRegistry().getDefaultProviderId()
                : "unknown";

        var sb = new StringBuilder("Session Status\n");
        sb.append("  ID:        ").append(session.id()).append("\n");
        sb.append("  Workspace: ").append(session.workspaceId()).append("\n");
        sb.append("  Status:    ").append(session.status()).append("\n");
        sb.append("  Created:   ").append(session.created()).append("\n");
        sb.append("  Last Active: ").append(session.lastActive()).append("\n");
        sb.append("  Transcript entries: ").append(turns).append("\n");
        sb.append("  Provider:  ").append(provider);
        return CommandResult.success(sb.toString());
    }
}
