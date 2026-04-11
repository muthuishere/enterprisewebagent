package com.enterprisewebagent.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "health", description = "Check server health status")
public class HealthCommand implements Runnable {

    @ParentCommand
    AgentCli parent;

    @Override
    public void run() {
        var client = new ServerClient(parent.serverUrl);
        try {
            var result = client.get("/actuator/health");
            OutputFormatter.printSuccess("Server is healthy: " + result);
        } catch (Exception e) {
            OutputFormatter.printError("Cannot reach server at " + parent.serverUrl);
            System.err.println("  " + e.getMessage());
        }
    }
}
