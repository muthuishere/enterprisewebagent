package com.enterprisewebagent.runtime.teams;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTeamRegistry implements TeamRegistry {

    private final ConcurrentHashMap<String, Team> teams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<TeamMessage>> messages = new ConcurrentHashMap<>();

    @Override
    public Team createTeam(String name, List<TeamMember> members) {
        String id = UUID.randomUUID().toString();
        Team team = new Team(id, name, List.copyOf(members), Instant.now());
        teams.put(id, team);
        messages.put(id, Collections.synchronizedList(new ArrayList<>()));
        return team;
    }

    @Override
    public void deleteTeam(String teamId) {
        Team removed = teams.remove(teamId);
        if (removed == null) {
            throw new IllegalArgumentException("Team not found: " + teamId);
        }
        messages.remove(teamId);
    }

    @Override
    public Optional<Team> getTeam(String teamId) {
        return Optional.ofNullable(teams.get(teamId));
    }

    @Override
    public List<Team> listTeams() {
        return List.copyOf(teams.values());
    }

    @Override
    public void sendMessage(String teamId, TeamMessage message) {
        List<TeamMessage> teamMessages = messages.get(teamId);
        if (teamMessages == null) {
            throw new IllegalArgumentException("Team not found: " + teamId);
        }
        teamMessages.add(message);
    }

    @Override
    public List<TeamMessage> getMessages(String teamId) {
        List<TeamMessage> teamMessages = messages.get(teamId);
        if (teamMessages == null) {
            throw new IllegalArgumentException("Team not found: " + teamId);
        }
        return List.copyOf(teamMessages);
    }
}
