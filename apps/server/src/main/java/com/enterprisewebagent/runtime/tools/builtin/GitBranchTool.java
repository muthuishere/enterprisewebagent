package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GitBranchTool implements ToolExecutor {

    private static final int TIMEOUT_SECONDS = 30;

    @Override
    public String toolName() {
        return "git_branch";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object actionObj = args.get("action");
        if (actionObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: action (list|create|switch|delete)", false);
        }

        String action = actionObj.toString().toLowerCase();
        Object nameObj = args.get("name");

        return switch (action) {
            case "list" -> executeGitCommand(List.of("git", "branch", "-a"));
            case "create" -> {
                if (nameObj == null || nameObj.toString().isBlank()) {
                    yield new ToolResult(toolName(), "Missing required parameter: name for create", false);
                }
                yield executeGitCommand(List.of("git", "branch", nameObj.toString()));
            }
            case "switch" -> {
                if (nameObj == null || nameObj.toString().isBlank()) {
                    yield new ToolResult(toolName(), "Missing required parameter: name for switch", false);
                }
                yield executeGitCommand(List.of("git", "checkout", nameObj.toString()));
            }
            case "delete" -> {
                if (nameObj == null || nameObj.toString().isBlank()) {
                    yield new ToolResult(toolName(), "Missing required parameter: name for delete", false);
                }
                yield executeGitCommand(List.of("git", "branch", "-d", nameObj.toString()));
            }
            default -> new ToolResult(toolName(), "Unknown action: " + action + ". Use: list, create, switch, delete", false);
        };
    }

    private ToolResult executeGitCommand(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            String toplevel = getGitToplevel();
            if (toplevel != null) {
                pb.directory(new java.io.File(toplevel));
            }

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolResult(toolName(), "Command timed out", false);
            }

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                return new ToolResult(toolName(), "Exit code " + exitCode + "\n" + stderr.trim(), false);
            }

            return new ToolResult(toolName(), stdout.isEmpty() ? "Branch operation completed" : stdout, true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Git not available: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolResult(toolName(), "Command interrupted", false);
        }
    }

    private String getGitToplevel() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--show-toplevel");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (p.waitFor(5, TimeUnit.SECONDS)) {
                return new String(p.getInputStream().readAllBytes()).trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
