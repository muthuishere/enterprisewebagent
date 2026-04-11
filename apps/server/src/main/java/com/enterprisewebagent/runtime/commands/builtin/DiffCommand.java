package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class DiffCommand implements Command {

    @Override
    public String name() {
        return "diff";
    }

    @Override
    public String description() {
        return "Show git diff";
    }

    @Override
    public String usage() {
        return "/diff [path]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        try {
            var cmdParts = new java.util.ArrayList<String>();
            cmdParts.add("git");
            cmdParts.add("diff");
            if (!context.args().isEmpty()) {
                cmdParts.add("--");
                cmdParts.add(context.args().getFirst());
            }

            var process = new ProcessBuilder(cmdParts)
                    .redirectErrorStream(true)
                    .start();

            String output;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return CommandResult.error("git diff failed (exit " + exitCode + "): " + output);
            }

            if (output.isBlank()) {
                return CommandResult.success("No changes detected.");
            }

            return CommandResult.success(output);
        } catch (Exception e) {
            return CommandResult.error("Failed to run git diff: " + e.getMessage());
        }
    }
}
