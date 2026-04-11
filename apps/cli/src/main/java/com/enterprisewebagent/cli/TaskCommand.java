package com.enterprisewebagent.cli;

import com.google.gson.reflect.TypeToken;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.Map;

@Command(
    name = "task",
    description = "Manage tasks",
    subcommands = {
        TaskCommand.ListCommand.class,
        TaskCommand.CreateCommand.class,
        TaskCommand.StatusCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class TaskCommand implements Runnable {

    @ParentCommand
    AgentCli parent;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "list", description = "List tasks for a session")
    static class ListCommand implements Runnable {
        @ParentCommand TaskCommand parent;

        @Parameters(index = "0", description = "Session ID")
        String sessionId;

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            var client = new ServerClient(parent.parent.serverUrl);
            try {
                var raw = client.get("/api/tasks?sessionId=" + sessionId);
                var tasks = (List<Map<String, Object>>) client.gson().fromJson(
                    raw, new TypeToken<List<Map<String, Object>>>() {}.getType()
                );
                if (tasks == null || tasks.isEmpty()) {
                    System.out.println("No tasks found for session " + sessionId);
                    return;
                }
                for (var task : tasks) {
                    OutputFormatter.printTask(task);
                }
            } catch (Exception e) {
                OutputFormatter.printError("Failed to list tasks: " + e.getMessage());
            }
        }
    }

    @Command(name = "create", description = "Create a task")
    static class CreateCommand implements Runnable {
        @ParentCommand TaskCommand parent;

        @Option(names = {"-s", "--session"}, required = true, description = "Session ID")
        String sessionId;

        @Parameters(index = "0", description = "Task description")
        String description;

        @Override
        public void run() {
            var client = new ServerClient(parent.parent.serverUrl);
            try {
                var result = client.postJson("/api/tasks",
                    Map.of("sessionId", sessionId, "description", description));
                OutputFormatter.printSuccess("Task created");
                OutputFormatter.printTask(result);
            } catch (Exception e) {
                OutputFormatter.printError("Failed to create task: " + e.getMessage());
            }
        }
    }

    @Command(name = "status", description = "Update task status")
    static class StatusCommand implements Runnable {
        @ParentCommand TaskCommand parent;

        @Parameters(index = "0", description = "Task ID")
        String taskId;

        @Parameters(index = "1", description = "New status (e.g. RUNNING, COMPLETED, FAILED)")
        String status;

        @Override
        public void run() {
            var client = new ServerClient(parent.parent.serverUrl);
            try {
                var result = client.putJson("/api/tasks/" + taskId + "/status",
                    Map.of("status", status));
                OutputFormatter.printSuccess("Task " + taskId + " status updated to " + status);
                OutputFormatter.printTask(result);
            } catch (Exception e) {
                OutputFormatter.printError("Failed to update task status: " + e.getMessage());
            }
        }
    }
}
