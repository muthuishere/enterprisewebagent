package com.enterprisewebagent.runtime.permissions;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionToolFilterTest {

    private final ToolContext ctx = new ToolContext("s1", "NORMAL", Set.of());

    private ToolDefinition tool(String name) {
        return new ToolDefinition(name, name + " tool", Map.of(), true);
    }

    @Test
    void lockedModeFiltersAllTools() {
        var evaluator = new PermissionEvaluator(PermissionMode.LOCKED,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());
        var filter = new PermissionToolFilter(evaluator);

        List<ToolDefinition> result = filter.filter(
            List.of(tool("shell"), tool("file_read"), tool("ask_user")), ctx);
        assertTrue(result.isEmpty());
    }

    @Test
    void unrestrictedModeKeepsAllTools() {
        var evaluator = new PermissionEvaluator(PermissionMode.UNRESTRICTED,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());
        var filter = new PermissionToolFilter(evaluator);

        List<ToolDefinition> tools = List.of(tool("shell"), tool("file_read"), tool("ask_user"));
        List<ToolDefinition> result = filter.filter(tools, ctx);
        assertEquals(3, result.size());
    }

    @Test
    void autoApproveKeepsAllToolsIncludingThoseNeedingApproval() {
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());
        var filter = new PermissionToolFilter(evaluator);

        List<ToolDefinition> tools = List.of(tool("shell"), tool("file_read"), tool("ask_user"));
        List<ToolDefinition> result = filter.filter(tools, ctx);
        // All tools should remain (shell included — approval is checked at execution time)
        assertEquals(3, result.size());
    }

    @Test
    void explicitDenyRuleRemovesTool() {
        var denyRule = new PermissionRule("deny-shell", PermissionType.DENY,
            PermissionScope.TOOL, "shell", "Block shell");
        var evaluator = new PermissionEvaluator(PermissionMode.AUTO_APPROVE,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of(denyRule));
        var filter = new PermissionToolFilter(evaluator);

        List<ToolDefinition> tools = List.of(tool("shell"), tool("file_read"), tool("ask_user"));
        List<ToolDefinition> result = filter.filter(tools, ctx);
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(t -> "shell".equals(t.name())));
    }

    @Test
    void getEvaluatorReturnsEvaluator() {
        var evaluator = new PermissionEvaluator(PermissionMode.UNRESTRICTED,
            new BashSafetyAnalyzer(), new FilePathChecker(), List.of());
        var filter = new PermissionToolFilter(evaluator);
        assertSame(evaluator, filter.getEvaluator());
    }
}
