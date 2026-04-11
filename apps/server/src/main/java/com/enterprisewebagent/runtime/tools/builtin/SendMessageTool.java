package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.teams.TeamMessage;
import com.enterprisewebagent.runtime.teams.TeamRegistry;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.time.Instant;
import java.util.Map;

public class SendMessageTool implements ToolExecutor {

    private final TeamRegistry teamRegistry;

    public SendMessageTool(TeamRegistry teamRegistry) {
        this.teamRegistry = teamRegistry;
    }

    @Override
    public String toolName() {
        return "send_message";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object teamIdObj = args.get("team_id");
        if (teamIdObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: team_id", false);
        }

        Object toObj = args.get("to");
        if (toObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: to", false);
        }

        Object contentObj = args.get("content");
        if (contentObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: content", false);
        }

        String teamId = teamIdObj.toString();
        String to = toObj.toString();
        String content = contentObj.toString();
        String from = args.containsKey("from") ? args.get("from").toString() : "coordinator";

        TeamMessage message = new TeamMessage(from, to, content, Instant.now());

        try {
            teamRegistry.sendMessage(teamId, message);
            return new ToolResult(toolName(),
                    "Message delivered to " + to + " in team " + teamId, true);
        } catch (IllegalArgumentException e) {
            return new ToolResult(toolName(), e.getMessage(), false);
        }
    }
}
