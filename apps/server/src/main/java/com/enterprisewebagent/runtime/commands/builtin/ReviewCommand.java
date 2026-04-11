package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

public class ReviewCommand implements Command {

    @Override
    public String name() {
        return "review";
    }

    @Override
    public String description() {
        return "Request code review of a file/directory";
    }

    @Override
    public String usage() {
        return "/review [path]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String path = context.args().isEmpty() ? "." : context.args().getFirst();
        String instruction = "Please review the code at path: " + path
                + ". Analyze for bugs, security issues, code quality, and suggest improvements.";
        return CommandResult.passThrough(instruction);
    }
}
