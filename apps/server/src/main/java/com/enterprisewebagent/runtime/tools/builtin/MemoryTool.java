package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.memory.MemoryFileLoader;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class MemoryTool implements ToolExecutor {

    private final MemoryFileLoader memoryFileLoader;
    private Path projectRoot;

    public MemoryTool(MemoryFileLoader memoryFileLoader) {
        this.memoryFileLoader = memoryFileLoader;
        this.projectRoot = Path.of(System.getProperty("user.dir"));
    }

    public MemoryTool(MemoryFileLoader memoryFileLoader, Path projectRoot) {
        this.memoryFileLoader = memoryFileLoader;
        this.projectRoot = projectRoot;
    }

    @Override
    public String toolName() {
        return "memory";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object actionObj = args.get("action");
        if (actionObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: action (show|add|clear|save)", false);
        }

        String action = actionObj.toString().toLowerCase();

        return switch (action) {
            case "show" -> handleShow();
            case "add" -> handleAdd(args);
            case "clear" -> handleClear();
            case "save" -> handleSave(args);
            default -> new ToolResult(toolName(), "Unknown action: " + action + ". Use: show, add, clear, save", false);
        };
    }

    private ToolResult handleShow() {
        var memory = memoryFileLoader.loadProjectMemory(projectRoot);
        if (memory.isEmpty()) {
            return new ToolResult(toolName(), "No MEMORY.md found in project", true);
        }
        return new ToolResult(toolName(), memory.get(), true);
    }

    private ToolResult handleAdd(Map<String, Object> args) {
        Object contentObj = args.get("content");
        if (contentObj == null || contentObj.toString().isBlank()) {
            return new ToolResult(toolName(), "Missing required parameter: content", false);
        }

        try {
            memoryFileLoader.appendToMemory(projectRoot, contentObj.toString());
            return new ToolResult(toolName(), "Entry added to MEMORY.md", true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Failed to append to memory: " + e.getMessage(), false);
        }
    }

    private ToolResult handleClear() {
        try {
            memoryFileLoader.saveProjectMemory(projectRoot, "# Project Memory\n\n");
            return new ToolResult(toolName(), "MEMORY.md cleared", true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Failed to clear memory: " + e.getMessage(), false);
        }
    }

    private ToolResult handleSave(Map<String, Object> args) {
        Object contentObj = args.get("content");
        if (contentObj == null || contentObj.toString().isBlank()) {
            return new ToolResult(toolName(), "Missing required parameter: content", false);
        }

        try {
            memoryFileLoader.saveProjectMemory(projectRoot, contentObj.toString());
            return new ToolResult(toolName(), "MEMORY.md saved", true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Failed to save memory: " + e.getMessage(), false);
        }
    }
}
