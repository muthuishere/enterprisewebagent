package com.enterprisewebagent.runtime.permissions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FilePathChecker {

    private static final List<String> SENSITIVE_PATHS = List.of(
        ".ssh/",
        ".aws/",
        ".env",
        "/etc/passwd",
        "/etc/shadow",
        ".gnupg/",
        ".kube/config"
    );

    private final List<PermissionRule> rules;

    public FilePathChecker(List<PermissionRule> rules) {
        this.rules = rules != null ? List.copyOf(rules) : List.of();
    }

    public FilePathChecker() {
        this(List.of());
    }

    public boolean isAllowed(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        String normalizedPath = normalizePath(path);

        // Check explicit rules first
        for (PermissionRule rule : rules) {
            if (rule.scope() != PermissionScope.FILE_PATH) continue;

            if (matchesPattern(normalizedPath, rule.pattern())) {
                return rule.type() == PermissionType.ALLOW;
            }
        }

        // Block sensitive paths by default
        for (String sensitive : SENSITIVE_PATHS) {
            if (normalizedPath.contains(sensitive)) {
                return false;
            }
        }

        return true;
    }

    public boolean isWithinProjectRoot(String path, String projectRoot) {
        if (path == null || projectRoot == null) {
            return false;
        }

        try {
            Path normalizedPath = Path.of(path).toAbsolutePath().normalize();
            Path normalizedRoot = Path.of(projectRoot).toAbsolutePath().normalize();
            return normalizedPath.startsWith(normalizedRoot);
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizePath(String path) {
        String home = System.getProperty("user.home");
        if (path.startsWith("~")) {
            return home + path.substring(1);
        }
        return path;
    }

    private boolean matchesPattern(String path, String pattern) {
        try {
            return Pattern.compile(pattern).matcher(path).find();
        } catch (Exception e) {
            return path.contains(pattern);
        }
    }
}
