package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;
import com.enterprisewebagent.runtime.session.Session;
import com.enterprisewebagent.runtime.session.SessionStatus;

import java.time.Instant;
import java.util.List;

public class ClearCommand implements Command {

    @Override
    public String name() {
        return "clear";
    }

    @Override
    public String description() {
        return "Clear conversation history for current session";
    }

    @Override
    public String usage() {
        return "/clear";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        var sessionOpt = context.sessionManager().get(context.sessionId());
        if (sessionOpt.isEmpty()) {
            return CommandResult.error("Session not found: " + context.sessionId());
        }
        var session = sessionOpt.get();
        // Close and recreate to clear transcript
        context.sessionManager().close(context.sessionId());
        var newSession = context.sessionManager().create(session.workspaceId());
        return CommandResult.success("Conversation cleared. New session: " + newSession.id());
    }
}
