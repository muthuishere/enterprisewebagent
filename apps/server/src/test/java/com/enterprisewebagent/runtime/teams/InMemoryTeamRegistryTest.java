package com.enterprisewebagent.runtime.teams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTeamRegistryTest {

    private InMemoryTeamRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryTeamRegistry();
    }

    @Test
    void createTeamAndRetrieve() {
        List<TeamMember> members = List.of(
                new TeamMember("agent-1", "coder", "gpt-4.1", Map.of()),
                new TeamMember("agent-2", "reviewer", "gpt-4.1", Map.of())
        );

        Team team = registry.createTeam("Alpha Team", members);

        assertNotNull(team.id());
        assertEquals("Alpha Team", team.name());
        assertEquals(2, team.members().size());
        assertNotNull(team.created());

        var retrieved = registry.getTeam(team.id());
        assertTrue(retrieved.isPresent());
        assertEquals(team.id(), retrieved.get().id());
    }

    @Test
    void deleteTeamRemovesIt() {
        Team team = registry.createTeam("Temp Team", List.of());
        registry.deleteTeam(team.id());

        assertTrue(registry.getTeam(team.id()).isEmpty());
    }

    @Test
    void deleteNonExistentTeamThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.deleteTeam("non-existent-id"));
    }

    @Test
    void listTeamsReturnsAll() {
        registry.createTeam("Team A", List.of());
        registry.createTeam("Team B", List.of());

        List<Team> teams = registry.listTeams();
        assertEquals(2, teams.size());
    }

    @Test
    void getNonExistentTeamReturnsEmpty() {
        assertTrue(registry.getTeam("missing").isEmpty());
    }

    @Test
    void sendAndGetMessages() {
        Team team = registry.createTeam("Messaging Team", List.of(
                new TeamMember("agent-1", "coder", "gpt-4.1", Map.of()),
                new TeamMember("agent-2", "reviewer", "gpt-4.1", Map.of())
        ));

        TeamMessage msg1 = new TeamMessage("agent-1", "agent-2", "Review this PR", Instant.now());
        TeamMessage msg2 = new TeamMessage("agent-2", "agent-1", "LGTM", Instant.now());

        registry.sendMessage(team.id(), msg1);
        registry.sendMessage(team.id(), msg2);

        List<TeamMessage> messages = registry.getMessages(team.id());
        assertEquals(2, messages.size());
        assertEquals("Review this PR", messages.get(0).content());
        assertEquals("LGTM", messages.get(1).content());
    }

    @Test
    void sendMessageToNonExistentTeamThrows() {
        TeamMessage msg = new TeamMessage("a", "b", "hello", Instant.now());
        assertThrows(IllegalArgumentException.class,
                () -> registry.sendMessage("missing", msg));
    }

    @Test
    void getMessagesFromNonExistentTeamThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.getMessages("missing"));
    }

    @Test
    void emptyTeamHasNoMessages() {
        Team team = registry.createTeam("Empty Team", List.of());
        assertTrue(registry.getMessages(team.id()).isEmpty());
    }

    @Test
    void deleteTeamRemovesMessages() {
        Team team = registry.createTeam("Del Team", List.of());
        registry.sendMessage(team.id(), new TeamMessage("a", "b", "msg", Instant.now()));
        registry.deleteTeam(team.id());

        assertThrows(IllegalArgumentException.class,
                () -> registry.getMessages(team.id()));
    }
}
