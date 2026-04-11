package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

public class FileReadTool implements ToolExecutor {

    @Override
    public String toolName() {
        return "file_read";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();
        Object pathObj = args.get("path");
        if (pathObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: path", false);
        }

        String filePath = pathObj.toString();
        try {
            String content = Files.readString(Path.of(filePath));
            return new ToolResult(toolName(), content, true);
        } catch (NoSuchFileException e) {
            return new ToolResult(toolName(), "File not found: " + filePath, false);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Error reading file: " + e.getMessage(), false);
        }
    }
}
