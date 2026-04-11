package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.teams.Team;
import com.enterprisewebagent.runtime.teams.TeamMember;
import com.enterprisewebagent.runtime.teams.TeamRegistry;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamCreateTool implements ToolExecutor {

    private final TeamRegistry teamRegistry;

    public TeamCreateTool(TeamRegistry teamRegistry) {
        this.teamRegistry = teamRegistry;
    }

    @Override
    public String toolName() {
        return "team_create";
    }

    @SuppressWarnings("unchecked")
    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object nameObj = args.get("name");
        if (nameObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: name", false);
        }

        Object membersObj = args.get("members");
        if (membersObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: members", false);
        }

        if (!(membersObj instanceof List<?> membersList)) {
            return new ToolResult(toolName(), "Parameter 'members' must be an array", false);
        }

        List<TeamMember> members = new ArrayList<>();
        for (Object item : membersList) {
            if (!(item instanceof Map<?, ?> memberMap)) {
                return new ToolResult(toolName(), "Each member must be an object with agentId, role, model", false);
            }

            String agentId = memberMap.containsKey("agentId") ? memberMap.get("agentId").toString() : null;
            String role = memberMap.containsKey("role") ? memberMap.get("role").toString() : null;
            String model = memberMap.containsKey("model") ? memberMap.get("model").toString() : "";

            if (agentId == null || role == null) {
                return new ToolResult(toolName(), "Each member must have agentId and role", false);
            }

            members.add(new TeamMember(agentId, role, model, Map.of()));
        }

        String name = nameObj.toString();
        Team team = teamRegistry.createTeam(name, members);

        StringBuilder output = new StringBuilder();
        output.append("Team created — id: ").append(team.id())
              .append(", name: ").append(team.name())
              .append(", members: [");
        for (int i = 0; i < team.members().size(); i++) {
            if (i > 0) output.append(", ");
            TeamMember m = team.members().get(i);
            output.append(m.agentId()).append("(").append(m.role()).append(")");
        }
        output.append("]");

        return new ToolResult(toolName(), output.toString(), true);
    }
}
