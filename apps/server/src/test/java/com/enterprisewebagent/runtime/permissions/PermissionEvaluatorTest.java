package com.enterprisewebagent.runtime.permissions;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionEvaluatorTest {

    private final ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());

    @Test
    void unrestrictedModeAllowsEverything() {
        var evaluator = new PermissionEvaluator(PermissionMode.UNRESTRICTED,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(new ToolInvocation("shell", Map.of("command", "rm -rf /")), ctx);
        assertTrue(decision.allowed());
        assertFalse(decision.requiresApproval());
    }

    @Test
    void lockedModeBlocksEverything() {
        var evaluator = new PermissionEvaluator(PermissionMode.LOCKED,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(new ToolInvocation("file_read", Map.of("path", "a.txt")), ctx);
        assertFalse(decision.allowed());
        assertFalse(decision.requiresApproval());
        assertTrue(decision.reason().contains("Locked"));
    }

    @Test
    void approveAllModeRequiresApproval() {
        var evaluator = new PermissionEvaluator(PermissionMode.APPROVE_ALL,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(new ToolInvocation("file_read", Map.of("path", "a.txt")), ctx);
        assertFalse(decision.allowed());
        assertTrue(decision.requiresApproval());
    }

    @Test
    void autoApproveSafeShellCommand() {
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(new ToolInvocation("shell", Map.of("command", "ls -la")), ctx);
        assertTrue(decision.allowed());
        assertFalse(decision.requiresApproval());
    }

    @Test
    void autoApproveDangerousShellBlocked() {
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(new ToolInvocation("shell", Map.of("command", "rm -rf /")), ctx);
        assertFalse(decision.allowed());
        assertFalse(decision.requiresApproval());
    }

    @Test
    void autoApproveModerateShellNeedsApproval() {
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(new ToolInvocation("shell", Map.of("command", "rm file.txt")), ctx);
        assertFalse(decision.allowed());
        assertTrue(decision.requiresApproval());
    }

    @Test
    void autoApproveSensitiveFilePathBlocked() {
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(
            new ToolInvocation("file_read", Map.of("path", "/home/user/.ssh/id_rsa")), ctx);
        assertFalse(decision.allowed());
    }

    @Test
    void autoApproveNormalFileAllowed() {
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(
            new ToolInvocation("file_read", Map.of("path", "/home/user/project/Main.java")), ctx);
        assertTrue(decision.allowed());
    }

    @Test
    void autoApproveNonShellNonFileToolAllowed() {
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());

        var decision = evaluator.evaluate(new ToolInvocation("ask_user", Map.of("question", "hello")), ctx);
        assertTrue(decision.allowed());
    }

    @Test
    void explicitToolDenyRuleBlocksTool() {
        var denyRule = new PermissionRule("deny-shell", PermissionType.DENY,
            PermissionScope.TOOL, "shell", "Block shell tool");
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of(denyRule));

        var decision = evaluator.evaluate(new ToolInvocation("shell", Map.of("command", "ls")), ctx);
        assertFalse(decision.allowed());
        assertFalse(decision.requiresApproval());
    }

    @Test
    void explicitToolAllowRuleAllowsTool() {
        var allowRule = new PermissionRule("allow-shell", PermissionType.ALLOW,
            PermissionScope.TOOL, "shell", "Allow shell tool");
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of(allowRule));

        // Even though the command might be dangerous, the explicit tool ALLOW rule takes precedence
        var decision = evaluator.evaluate(new ToolInvocation("shell", Map.of("command", "rm -rf /")), ctx);
        assertTrue(decision.allowed());
    }

    @Test
    void getModeReturnsConfiguredMode() {
        var evaluator = new PermissionEvaluator(PermissionMode.LOCKED,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());
        assertEquals(PermissionMode.LOCKED, evaluator.getMode());
    }
}
