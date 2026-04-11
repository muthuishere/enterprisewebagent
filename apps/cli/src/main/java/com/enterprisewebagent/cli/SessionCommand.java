package com.enterprisewebagent.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

@Command(
    name = "session",
    description = "Manage agent sessions",
    subcommands = {
        SessionCommand.ListCommand.class,
        SessionCommand.CreateCommand.class,
        SessionCommand.ChatCommand.class,
        SessionCommand.CloseCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class SessionCommand implements Runnable {

    @ParentCommand
    AgentCli parent;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "list", description = "List all sessions")
    static class ListCommand implements Runnable {
        @ParentCommand SessionCommand parent;

        @Override
        public void run() {
            var client = new ServerClient(parent.parent.serverUrl);
            try {
                var result = client.get("/api/sessions");
                System.out.println(result);
            } catch (Exception e) {
                OutputFormatter.printError("Failed to list sessions: " + e.getMessage());
            }
        }
    }

    @Command(name = "create", description = "Create a new session")
    static class CreateCommand implements Runnable {
        @ParentCommand SessionCommand parent;

        @Option(names = {"-w", "--workspace"}, description = "Workspace ID", defaultValue = "default")
        String workspaceId;

        @Override
        public void run() {
            var client = new ServerClient(parent.parent.serverUrl);
            try {
                var result = client.postJson("/api/sessions", Map.of("workspaceId", workspaceId));
                OutputFormatter.printSuccess("Session created");
                OutputFormatter.printSession(result);
            } catch (Exception e) {
                OutputFormatter.printError("Failed to create session: " + e.getMessage());
            }
        }
    }

    @Command(name = "chat", description = "Interactive chat with the agent")
    static class ChatCommand implements Runnable {
        @ParentCommand SessionCommand parent;

        @Option(names = {"-s", "--session"}, description = "Session ID (creates new if omitted)")
        String sessionId;

        @Override
        public void run() {
            var client = new ServerClient(parent.parent.serverUrl);
            try {
                // Create or resume session
                if (sessionId == null || sessionId.isBlank()) {
                    var session = client.postJson("/api/sessions", Map.of("workspaceId", "default"));
                    sessionId = String.valueOf(session.get("id"));
                    OutputFormatter.printSuccess("Created session: " + sessionId);
                } else {
                    OutputFormatter.printSuccess("Resuming session: " + sessionId);
                }

                System.out.println("Type your message (\"exit\" or \"quit\" to end):");
                System.out.println();

                var reader = new BufferedReader(new InputStreamReader(System.in));
                String line;
                while (true) {
                    System.out.print("you> ");
                    System.out.flush();
                    line = reader.readLine();
                    if (line == null || line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                        break;
                    }
                    if (line.isBlank()) {
                        continue;
                    }
                    try {
                        var turnResult = client.postJson(
                            "/api/sessions/" + sessionId + "/turns",
                            Map.of("input", line)
                        );
                        var output = turnResult.getOrDefault("output", turnResult.getOrDefault("response", ""));
                        OutputFormatter.printAgentResponse(String.valueOf(output));
                    } catch (Exception e) {
                        OutputFormatter.printError("Turn failed: " + e.getMessage());
                    }
                }

                // Close session on exit
                try {
                    client.post("/api/sessions/" + sessionId + "/close", Map.of());
                    OutputFormatter.printSuccess("Session closed.");
                } catch (Exception e) {
                    // Best-effort close
                }
            } catch (Exception e) {
                OutputFormatter.printError("Chat failed: " + e.getMessage());
            }
        }
    }

    @Command(name = "close", description = "Close a session")
    static class CloseCommand implements Runnable {
        @ParentCommand SessionCommand parent;

        @Parameters(index = "0", description = "Session ID")
        String sessionId;

        @Override
        public void run() {
            var client = new ServerClient(parent.parent.serverUrl);
            try {
                client.post("/api/sessions/" + sessionId + "/close", Map.of());
                OutputFormatter.printSuccess("Session " + sessionId + " closed.");
            } catch (Exception e) {
                OutputFormatter.printError("Failed to close session: " + e.getMessage());
            }
        }
    }
}
