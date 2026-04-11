package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

public class GlobTool implements ToolExecutor {

    private static final int MAX_RESULTS = 1000;

    @Override
    public String toolName() {
        return "glob";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object patternObj = args.get("pattern");
        if (patternObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: pattern", false);
        }

        String pattern = patternObj.toString();
        String basePath = args.containsKey("path") ? args.get("path").toString() : System.getProperty("user.dir");

        Path root = Path.of(basePath);
        if (!Files.isDirectory(root)) {
            return new ToolResult(toolName(), "Path is not a directory: " + basePath, false);
        }

        try {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            List<String> matches = new ArrayList<>();

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(root) && dir.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return matches.size() >= MAX_RESULTS ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matches.size() >= MAX_RESULTS) {
                        return FileVisitResult.TERMINATE;
                    }
                    Path relative = root.relativize(file);
                    if (matcher.matches(relative) || matcher.matches(file.getFileName())) {
                        matches.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            if (matches.isEmpty()) {
                return new ToolResult(toolName(), "No files matched pattern: " + pattern, true);
            }

            return new ToolResult(toolName(), String.join("\n", matches), true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Error walking file tree: " + e.getMessage(), false);
        } catch (PatternSyntaxException e) {
            return new ToolResult(toolName(), "Invalid glob pattern: " + e.getMessage(), false);
        }
    }
}
