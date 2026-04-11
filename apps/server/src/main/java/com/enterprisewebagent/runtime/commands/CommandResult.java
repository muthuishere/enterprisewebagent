package com.enterprisewebagent.runtime.commands;

public record CommandResult(String output, boolean success, boolean suppressTurn) {

    public static CommandResult success(String output) {
        return new CommandResult(output, true, true);
    }

    public static CommandResult error(String output) {
        return new CommandResult(output, false, true);
    }

    public static CommandResult passThrough(String output) {
        return new CommandResult(output, true, false);
    }
}
