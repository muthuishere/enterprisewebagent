package com.enterprisewebagent.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "agent",
    mixinStandardHelpOptions = true,
    version = "enterprisewebagent-cli 0.0.1",
    description = "Enterprise Web Agent CLI — terminal surface for the agent runtime.",
    subcommands = {
        HealthCommand.class,
        SessionCommand.class,
        TaskCommand.class,
        ConfigCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class AgentCli implements Runnable {

    @Option(names = {"--server"}, description = "Server URL (default: http://localhost:8080)", defaultValue = "http://localhost:8080")
    String serverUrl;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AgentCli()).execute(args);
        System.exit(exitCode);
    }
}
