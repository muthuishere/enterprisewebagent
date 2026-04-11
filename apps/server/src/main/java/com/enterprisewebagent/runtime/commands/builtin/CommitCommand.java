package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class CommitCommand implements Command {

    @Override
    public String name() {
        return "commit";
    }

    @Override
    public String description() {
        return "Git commit with optional message";
    }

    @Override
    public String usage() {
        return "/commit [message]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String message = context.args().isEmpty()
                ? "Update via agent"
                : String.join(" ", context.args());

        try {
            // Stage all changes first
            var addProcess = new ProcessBuilder("git", "add", "-A")
                    .redirectErrorStream(true)
                    .start();
            addProcess.waitFor();

            // Commit
            var commitProcess = new ProcessBuilder("git", "commit", "-m", message)
                    .redirectErrorStream(true)
                    .start();

            String output;
            try (var reader = new BufferedReader(new InputStreamReader(commitProcess.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = commitProcess.waitFor();
            if (exitCode != 0) {
                return CommandResult.error("git commit failed (exit " + exitCode + "): " + output);
            }

            return CommandResult.success(output);
        } catch (Exception e) {
            return CommandResult.error("Failed to run git commit: " + e.getMessage());
        }
    }
}
