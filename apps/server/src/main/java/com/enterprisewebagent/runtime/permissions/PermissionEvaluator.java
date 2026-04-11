package com.enterprisewebagent.runtime.permissions;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;

import java.util.List;
import java.util.Map;

public class PermissionEvaluator {

    private final PermissionMode mode;
    private final BashSafetyAnalyzer bashAnalyzer;
    private final FilePathChecker pathChecker;
    private final List<PermissionRule> rules;

    public PermissionEvaluator(PermissionMode mode,
                                BashSafetyAnalyzer bashAnalyzer,
                                FilePathChecker pathChecker,
                                List<PermissionRule> rules) {
        this.mode = mode;
        this.bashAnalyzer = bashAnalyzer;
        this.pathChecker = pathChecker;
        this.rules = rules != null ? List.copyOf(rules) : List.of();
    }

    public PermissionDecision evaluate(ToolInvocation invocation, ToolContext context) {
        return switch (mode) {
            case UNRESTRICTED -> new PermissionDecision(true, "Unrestricted mode", false);
            case LOCKED -> new PermissionDecision(false, "Locked mode — all tool execution blocked", false);
            case APPROVE_ALL -> new PermissionDecision(false, "Approval required for: " + invocation.name(), true);
            case AUTO_APPROVE -> evaluateAutoApprove(invocation, context);
        };
    }

    private PermissionDecision evaluateAutoApprove(ToolInvocation invocation, ToolContext context) {
        // Check explicit rules first
        for (PermissionRule rule : rules) {
            if (rule.scope() == PermissionScope.TOOL && matchesToolName(invocation.name(), rule.pattern())) {
                return switch (rule.type()) {
                    case ALLOW -> new PermissionDecision(true, "Allowed by rule: " + rule.description(), false);
                    case DENY -> new PermissionDecision(false, "Denied by rule: " + rule.description(), false);
                    case ASK -> new PermissionDecision(false, "Approval required by rule: " + rule.description(), true);
                };
            }
        }

        // Check bash commands for safety
        if ("shell".equals(invocation.name())) {
            return evaluateShellCommand(invocation);
        }

        // Check file path operations
        if (isFileOperation(invocation.name())) {
            return evaluateFilePath(invocation);
        }

        // Default: auto-approve safe tools
        return new PermissionDecision(true, "Auto-approved: " + invocation.name(), false);
    }

    private PermissionDecision evaluateShellCommand(ToolInvocation invocation) {
        Map<String, Object> args = invocation.arguments();
        String command = args != null ? String.valueOf(args.getOrDefault("command", "")) : "";

        // Check bash command rules
        for (PermissionRule rule : rules) {
            if (rule.scope() == PermissionScope.BASH_COMMAND) {
                if (command.matches(rule.pattern()) || command.contains(rule.pattern())) {
                    return switch (rule.type()) {
                        case ALLOW -> new PermissionDecision(true, "Allowed by bash rule: " + rule.description(), false);
                        case DENY -> new PermissionDecision(false, "Denied by bash rule: " + rule.description(), false);
                        case ASK -> new PermissionDecision(false, "Approval required by bash rule: " + rule.description(), true);
                    };
                }
            }
        }

        BashSafetyAnalyzer.SafetyLevel safety = bashAnalyzer.analyze(command);
        return switch (safety) {
            case SAFE -> new PermissionDecision(true, "Auto-approved: safe shell command", false);
            case MODERATE -> new PermissionDecision(false, "Approval required: moderate-risk command — " + bashAnalyzer.explain(command), true);
            case DANGEROUS -> new PermissionDecision(false, "Blocked: dangerous command — " + bashAnalyzer.explain(command), false);
        };
    }

    private PermissionDecision evaluateFilePath(ToolInvocation invocation) {
        Map<String, Object> args = invocation.arguments();
        String path = args != null ? String.valueOf(args.getOrDefault("path", "")) : "";

        // Check file path rules
        for (PermissionRule rule : rules) {
            if (rule.scope() == PermissionScope.FILE_PATH) {
                if (path.matches(rule.pattern()) || path.contains(rule.pattern())) {
                    return switch (rule.type()) {
                        case ALLOW -> new PermissionDecision(true, "Allowed by path rule: " + rule.description(), false);
                        case DENY -> new PermissionDecision(false, "Denied by path rule: " + rule.description(), false);
                        case ASK -> new PermissionDecision(false, "Approval required by path rule: " + rule.description(), true);
                    };
                }
            }
        }

        if (!pathChecker.isAllowed(path)) {
            return new PermissionDecision(false, "Blocked: access to sensitive path", false);
        }

        return new PermissionDecision(true, "Auto-approved: file path allowed", false);
    }

    private boolean matchesToolName(String toolName, String pattern) {
        try {
            return toolName.matches(pattern);
        } catch (Exception e) {
            return toolName.equals(pattern);
        }
    }

    private boolean isFileOperation(String toolName) {
        return "file_read".equals(toolName)
            || "file_edit".equals(toolName)
            || "file_write".equals(toolName);
    }

    public PermissionMode getMode() {
        return mode;
    }

    public List<PermissionRule> getRules() {
        return rules;
    }
}
