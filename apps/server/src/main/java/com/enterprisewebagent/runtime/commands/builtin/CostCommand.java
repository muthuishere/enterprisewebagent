package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

public class CostCommand implements Command {

    @Override
    public String name() {
        return "cost";
    }

    @Override
    public String description() {
        return "Show token usage and estimated cost for session";
    }

    @Override
    public String usage() {
        return "/cost";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        var sessionOpt = context.sessionManager().get(context.sessionId());
        if (sessionOpt.isEmpty()) {
            return CommandResult.error("Session not found: " + context.sessionId());
        }
        var session = sessionOpt.get();
        int turns = session.transcript().size();
        var sb = new StringBuilder("Cost Summary\n");
        sb.append("  Session:    ").append(session.id()).append("\n");
        sb.append("  Turns:      ").append(turns).append("\n");
        sb.append("  Tokens:     (tracking not yet implemented)\n");
        sb.append("  Est. cost:  (tracking not yet implemented)");
        return CommandResult.success(sb.toString());
    }
}
