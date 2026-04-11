package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellTool implements ToolExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public String toolName() {
        return "shell";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object commandObj = args.get("command");
        if (commandObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: command", false);
        }

        String command = commandObj.toString();
        int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        Object timeoutObj = args.get("timeout_seconds");
        if (timeoutObj != null) {
            try {
                timeoutSeconds = Integer.parseInt(timeoutObj.toString());
            } catch (NumberFormatException e) {
                // use default
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolResult(toolName(), "Command timed out after " + timeoutSeconds + " seconds", false);
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                return new ToolResult(toolName(), "Exit code " + exitCode + "\n" + output, false);
            }

            return new ToolResult(toolName(), output, true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Error executing command: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolResult(toolName(), "Command interrupted", false);
        }
    }
}
