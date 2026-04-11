package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.teams.TeamRegistry;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.Map;

public class TeamDeleteTool implements ToolExecutor {

    private final TeamRegistry teamRegistry;

    public TeamDeleteTool(TeamRegistry teamRegistry) {
        this.teamRegistry = teamRegistry;
    }

    @Override
    public String toolName() {
        return "team_delete";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object teamIdObj = args.get("team_id");
        if (teamIdObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: team_id", false);
        }

        String teamId = teamIdObj.toString();
        try {
            teamRegistry.deleteTeam(teamId);
            return new ToolResult(toolName(), "Team deleted: " + teamId, true);
        } catch (IllegalArgumentException e) {
            return new ToolResult(toolName(), e.getMessage(), false);
        }
    }
}
