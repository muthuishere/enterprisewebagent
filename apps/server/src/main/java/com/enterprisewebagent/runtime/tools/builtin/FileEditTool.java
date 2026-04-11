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

public class FileEditTool implements ToolExecutor {

    @Override
    public String toolName() {
        return "file_edit";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object pathObj = args.get("path");
        Object oldStrObj = args.get("old_str");
        Object newStrObj = args.get("new_str");

        if (pathObj == null || oldStrObj == null || newStrObj == null) {
            return new ToolResult(toolName(), "Missing required parameters: path, old_str, new_str", false);
        }

        String filePath = pathObj.toString();
        String oldStr = oldStrObj.toString();
        String newStr = newStrObj.toString();

        try {
            String content = Files.readString(Path.of(filePath));

            int firstIndex = content.indexOf(oldStr);
            if (firstIndex == -1) {
                return new ToolResult(toolName(), "old_str not found in file: " + filePath, false);
            }

            int secondIndex = content.indexOf(oldStr, firstIndex + 1);
            if (secondIndex != -1) {
                return new ToolResult(toolName(), "old_str found multiple times in file — edit is ambiguous: " + filePath, false);
            }

            String updated = content.substring(0, firstIndex) + newStr + content.substring(firstIndex + oldStr.length());
            Files.writeString(Path.of(filePath), updated);

            return new ToolResult(toolName(), "File edited successfully: " + filePath, true);
        } catch (NoSuchFileException e) {
            return new ToolResult(toolName(), "File not found: " + filePath, false);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Error editing file: " + e.getMessage(), false);
        }
    }
}
