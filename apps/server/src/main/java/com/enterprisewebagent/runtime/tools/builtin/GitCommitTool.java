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

public class GitCommitTool implements ToolExecutor {

    private static final int TIMEOUT_SECONDS = 30;

    @Override
    public String toolName() {
        return "git_commit";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object messageObj = args.get("message");
        if (messageObj == null || messageObj.toString().isBlank()) {
            return new ToolResult(toolName(), "Missing required parameter: message", false);
        }

        String gitToplevel = getGitToplevel();

        // Stage files if provided
        Object filesObj = args.get("files");
        if (filesObj != null) {
            List<String> files = parseFileList(filesObj);
            if (!files.isEmpty()) {
                ToolResult stageResult = stageFiles(files, gitToplevel);
                if (!stageResult.success()) {
                    return stageResult;
                }
            }
        }

        // Execute commit
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("commit");
        command.add("-m");
        command.add(messageObj.toString());

        return executeGitCommand(command, gitToplevel);
    }

    private ToolResult stageFiles(List<String> files, String toplevel) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("add");
        command.addAll(files);
        return executeGitCommand(command, toplevel);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseFileList(Object filesObj) {
        if (filesObj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(filesObj.toString().split("[,\\s]+"));
    }

    private ToolResult executeGitCommand(List<String> command, String toplevel) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
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

            return new ToolResult(toolName(), stdout, true);
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
