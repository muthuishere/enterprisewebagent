package com.enterprisewebagent.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "config", description = "Show runtime configuration")
public class ConfigCommand implements Runnable {

    @ParentCommand
    AgentCli parent;

    @Override
    public void run() {
        var client = new ServerClient(parent.serverUrl);
        try {
            var config = client.getJson("/api/config");
            OutputFormatter.printSuccess("Runtime configuration:");
            OutputFormatter.printConfig(config);
        } catch (Exception e) {
            OutputFormatter.printError("Failed to fetch config: " + e.getMessage());
        }
    }
}
