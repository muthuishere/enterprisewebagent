package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

public class VersionCommand implements Command {

    private static final String VERSION = "0.0.1-SNAPSHOT";
    private static final String RUNTIME = "Enterprise Web Agent";

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return "Show version info";
    }

    @Override
    public String usage() {
        return "/version";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String javaVersion = System.getProperty("java.version", "unknown");
        var sb = new StringBuilder();
        sb.append(RUNTIME).append(" v").append(VERSION).append("\n");
        sb.append("  Java:    ").append(javaVersion).append("\n");
        sb.append("  Runtime: Spring Boot");
        return CommandResult.success(sb.toString());
    }
}
