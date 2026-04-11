package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.Map;

public class SleepTool implements ToolExecutor {

    private static final int MAX_SECONDS = 300;

    @Override
    public String toolName() {
        return "sleep";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object secondsObj = args.get("seconds");
        if (secondsObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: seconds", false);
        }

        double seconds;
        try {
            seconds = Double.parseDouble(secondsObj.toString());
        } catch (NumberFormatException e) {
            return new ToolResult(toolName(), "Invalid seconds value: " + secondsObj, false);
        }

        if (seconds < 0) {
            return new ToolResult(toolName(), "Seconds must be non-negative", false);
        }

        if (seconds > MAX_SECONDS) {
            return new ToolResult(toolName(), "Seconds exceeds maximum of " + MAX_SECONDS, false);
        }

        try {
            long millis = (long) (seconds * 1000);
            Thread.sleep(millis);
            return new ToolResult(toolName(), "Slept for " + seconds + " seconds", true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolResult(toolName(), "Sleep interrupted", false);
        }
    }
}
