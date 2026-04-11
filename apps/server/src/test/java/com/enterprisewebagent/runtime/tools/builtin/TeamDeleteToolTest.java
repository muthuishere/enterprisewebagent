package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.teams.InMemoryTeamRegistry;
import com.enterprisewebagent.runtime.teams.Team;
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

class TeamDeleteToolTest {

    private TeamDeleteTool tool;
    private TeamRegistry teamRegistry;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        teamRegistry = new InMemoryTeamRegistry();
        tool = new TeamDeleteTool(teamRegistry);
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void deletesExistingTeam() {
        Team team = teamRegistry.createTeam("To Delete", List.of());

        ToolInvocation inv = new ToolInvocation("team_delete",
                Map.of("team_id", team.id()));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("Team deleted"));
        assertTrue(teamRegistry.getTeam(team.id()).isEmpty());
    }

    @Test
    void deleteNonExistentTeamReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("team_delete",
                Map.of("team_id", "non-existent"));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Team not found"));
    }

    @Test
    void missingTeamIdReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("team_delete", Map.of());
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: team_id"));
    }
}
