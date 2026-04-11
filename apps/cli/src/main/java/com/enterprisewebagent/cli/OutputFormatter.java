package com.enterprisewebagent.cli;

import java.util.List;
import java.util.Map;

public class OutputFormatter {

    public static void printSuccess(String message) {
        System.out.println("✓ " + message);
    }

    public static void printError(String message) {
        System.err.println("✗ " + message);
    }

    public static void printSession(Map<String, Object> session) {
        System.out.printf("Session: %s [%s]%n", session.get("id"), session.get("status"));
        System.out.printf("  Workspace: %s%n", session.get("workspaceId"));
        System.out.printf("  Created: %s%n", session.get("created"));
    }

    public static void printTask(Map<String, Object> task) {
        System.out.printf("Task: %s [%s]%n", task.get("id"), task.get("status"));
        System.out.printf("  %s%n", task.get("description"));
    }

    @SuppressWarnings("unchecked")
    public static void printTaskList(List<Map<String, Object>> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }
        for (var task : tasks) {
            printTask(task);
        }
    }

    public static void printAgentResponse(String output) {
        System.out.println();
        System.out.println(output);
        System.out.println();
    }

    public static void printConfig(Map<String, Object> config) {
        for (var entry : config.entrySet()) {
            System.out.printf("  %s: %s%n", entry.getKey(), entry.getValue());
        }
    }
}
