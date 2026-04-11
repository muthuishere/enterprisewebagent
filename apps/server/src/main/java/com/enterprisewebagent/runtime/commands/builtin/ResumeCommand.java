package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

public class ResumeCommand implements Command {

    @Override
    public String name() {
        return "resume";
    }

    @Override
    public String description() {
        return "Resume a previous session";
    }

    @Override
    public String usage() {
        return "/resume <sessionId>";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.args().isEmpty()) {
            return CommandResult.error("Usage: /resume <sessionId>");
        }
        String targetId = context.args().getFirst();
        try {
            var session = context.sessionManager().resume(targetId);
            return CommandResult.success("Resumed session: " + session.id()
                    + " (transcript entries: " + session.transcript().size() + ")");
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Cannot resume session: " + e.getMessage());
        }
    }
}
