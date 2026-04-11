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

class SendMessageToolTest {

    private SendMessageTool tool;
    private TeamRegistry teamRegistry;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        teamRegistry = new InMemoryTeamRegistry();
        tool = new SendMessageTool(teamRegistry);
        context = new ToolContext("s1", "NORMAL", Set.of());
    }

    @Test
    void sendsMessageSuccessfully() {
        Team team = teamRegistry.createTeam("Messaging Team", List.of());

        ToolInvocation inv = new ToolInvocation("send_message",
                Map.of("team_id", team.id(), "to", "agent-1", "content", "Hello!"));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("Message delivered to agent-1"));

        var messages = teamRegistry.getMessages(team.id());
        assertEquals(1, messages.size());
        assertEquals("Hello!", messages.get(0).content());
        assertEquals("coordinator", messages.get(0).from());
        assertEquals("agent-1", messages.get(0).to());
    }

    @Test
    void sendsMessageWithCustomFrom() {
        Team team = teamRegistry.createTeam("Team", List.of());

        ToolInvocation inv = new ToolInvocation("send_message",
                Map.of("team_id", team.id(), "to", "agent-2", "content", "Hi",
                        "from", "agent-1"));
        ToolResult result = tool.execute(inv, context);

        assertTrue(result.success());

        var messages = teamRegistry.getMessages(team.id());
        assertEquals("agent-1", messages.get(0).from());
    }

    @Test
    void missingTeamIdReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("send_message",
                Map.of("to", "agent-1", "content", "Hello"));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: team_id"));
    }

    @Test
    void missingToReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("send_message",
                Map.of("team_id", "t1", "content", "Hello"));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: to"));
    }

    @Test
    void missingContentReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("send_message",
                Map.of("team_id", "t1", "to", "agent-1"));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter: content"));
    }

    @Test
    void sendToNonExistentTeamReturnsFalse() {
        ToolInvocation inv = new ToolInvocation("send_message",
                Map.of("team_id", "non-existent", "to", "agent-1", "content", "Hi"));
        ToolResult result = tool.execute(inv, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Team not found"));
    }
}
