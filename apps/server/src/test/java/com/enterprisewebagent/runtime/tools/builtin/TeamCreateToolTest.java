package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.teams.InMemoryTeamRegistry;
import com.enterprisewebagent.runtime.teams.TeamRegistry;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TeamCreateToolTest {

    private TeamCreateTool tool;
    private TeamRegistry teamRegistry;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        teamRegistry = new InMemoryTeamRegistry();
        tool = new TeamCreateTool(teamRegistry);
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void createsTeamWithMembers() {
        List<Map<String, Object>> members = List.of(
                Map.of("agentId", "agent-1", "role", "coder", "model", "gpt-4.1"),
                Map.of("agentId", "agent-2", "role", "reviewer", "model", "gpt-4.1")
        );

        ToolInvocation inv = new ToolInvocation("team_create",
                Map.of("name", "Alpha", "members", members));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("Team created"));
        assertTrue(result.output().contains("Alpha"));
        assertTrue(result.output().contains("agent-1(coder)"));
        assertTrue(result.output().contains("agent-2(reviewer)"));

        assertEquals(1, teamRegistry.listTeams().size());
    }

    @Test
    void missingNameReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("team_create",
                Map.of("members", List.of()));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: name"));
    }

    @Test
    void missingMembersReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("team_create",
                Map.of("name", "Team"));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: members"));
    }

    @Test
    void invalidMemberMissingAgentId() {
        List<Map<String, Object>> members = List.of(
                Map.of("role", "coder")
        );

        ToolInvocation inv = new ToolInvocation("team_create",
                Map.of("name", "Bad", "members", members));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("agentId and role"));
    }

    @Test
    void emptyMembersListSucceeds() {
        ToolInvocation inv = new ToolInvocation("team_create",
                Map.of("name", "Empty Team", "members", List.of()));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("Team created"));
    }
}
