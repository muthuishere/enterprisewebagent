package com.enterprisewebagent.runtime.commands;

public interface Command {
    String name();
    String description();
    String usage();
    CommandResult execute(CommandContext context);
}
