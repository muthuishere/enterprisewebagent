package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileWriteTool implements ToolExecutor {

    @Override
    public String toolName() {
        return "file_write";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object pathObj = args.get("path");
        Object contentObj = args.get("content");

        if (pathObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: path", false);
        }
        if (contentObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: content", false);
        }

        String filePath = pathObj.toString();
        String content = contentObj.toString();
        Path target = Path.of(filePath);

        boolean createDirs = true;
        if (args.containsKey("create_dirs")) {
            Object createDirsObj = args.get("create_dirs");
            createDirs = Boolean.parseBoolean(createDirsObj.toString());
        }

        if (Files.exists(target)) {
            return new ToolResult(toolName(), "File already exists: " + filePath, false);
        }

        try {
            if (createDirs && target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            Files.write(target, bytes);

            return new ToolResult(toolName(), "File written: " + filePath + " (" + bytes.length + " bytes)", true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Error writing file: " + e.getMessage(), false);
        }
    }
}
