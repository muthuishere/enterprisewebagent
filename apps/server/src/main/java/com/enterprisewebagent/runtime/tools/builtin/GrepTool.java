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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GrepTool implements ToolExecutor {

    private static final int MAX_MATCHES = 500;

    @Override
    public String toolName() {
        return "grep";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object patternObj = args.get("pattern");
        if (patternObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: pattern", false);
        }

        Pattern regex;
        try {
            regex = Pattern.compile(patternObj.toString());
        } catch (PatternSyntaxException e) {
            return new ToolResult(toolName(), "Invalid regex pattern: " + e.getMessage(), false);
        }

        String basePath = args.containsKey("path") ? args.get("path").toString() : System.getProperty("user.dir");
        Path root = Path.of(basePath);

        PathMatcher includeMatcher = null;
        if (args.containsKey("include")) {
            String includePattern = args.get("include").toString();
            includeMatcher = FileSystems.getDefault().getPathMatcher("glob:" + includePattern);
        }

        if (!Files.exists(root)) {
            return new ToolResult(toolName(), "Path does not exist: " + basePath, false);
        }

        List<String> results = new ArrayList<>();
        PathMatcher finalIncludeMatcher = includeMatcher;

        try {
            if (Files.isRegularFile(root)) {
                searchFile(root, regex, results);
            } else {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!dir.equals(root) && dir.getFileName().toString().startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return results.size() >= MAX_MATCHES ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (results.size() >= MAX_MATCHES) {
                            return FileVisitResult.TERMINATE;
                        }
                        if (finalIncludeMatcher != null && !finalIncludeMatcher.matches(file.getFileName())) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (isBinaryFile(file)) {
                            return FileVisitResult.CONTINUE;
                        }
                        searchFile(file, regex, results);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            return new ToolResult(toolName(), "Error searching files: " + e.getMessage(), false);
        }

        if (results.isEmpty()) {
            return new ToolResult(toolName(), "No matches found for pattern: " + patternObj, true);
        }

        return new ToolResult(toolName(), String.join("\n", results), true);
    }

    private void searchFile(Path file, Pattern regex, List<String> results) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size() && results.size() < MAX_MATCHES; i++) {
                Matcher matcher = regex.matcher(lines.get(i));
                if (matcher.find()) {
                    results.add(file + ":" + (i + 1) + ":" + lines.get(i));
                }
            }
        } catch (IOException e) {
            // skip unreadable files
        }
    }

    private boolean isBinaryFile(Path file) {
        try {
            String contentType = Files.probeContentType(file);
            if (contentType != null && !contentType.startsWith("text/")) {
                return true;
            }
            // Check first 512 bytes for null characters
            byte[] bytes = new byte[512];
            try (var is = Files.newInputStream(file)) {
                int read = is.read(bytes);
                if (read > 0) {
                    for (int i = 0; i < read; i++) {
                        if (bytes[i] == 0) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
