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

public class GitDiffTool implements ToolExecutor {

    private static final int TIMEOUT_SECONDS = 30;

    @Override
    public String toolName() {
        return "git_diff";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("diff");

        Object stagedObj = args.get("staged");
        if (stagedObj != null && Boolean.parseBoolean(stagedObj.toString())) {
            command.add("--staged");
        }

        Object commitObj = args.get("commit");
        if (commitObj != null && !commitObj.toString().isBlank()) {
            command.add(commitObj.toString());
        }

        Object pathObj = args.get("path");
        if (pathObj != null && !pathObj.toString().isBlank()) {
            command.add("--");
            command.add(pathObj.toString());
        }

        return executeGitCommand(command);
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

            return new ToolResult(toolName(), stdout.isEmpty() ? "No differences found" : stdout, true);
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
