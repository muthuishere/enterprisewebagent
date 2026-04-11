package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GitStatusTool implements ToolExecutor {

    private static final int TIMEOUT_SECONDS = 30;

    @Override
    public String toolName() {
        return "git_status";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        return executeGitCommand(List.of("git", "status", "--porcelain"));
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

            return new ToolResult(toolName(), stdout.isEmpty() ? "Working tree clean" : stdout, true);
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
