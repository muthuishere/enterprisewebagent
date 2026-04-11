package com.enterprisewebagent.runtime.teams;

import java.util.List;
import java.util.Optional;

public interface TeamRegistry {
    Team createTeam(String name, List<TeamMember> members);
    void deleteTeam(String teamId);
    Optional<Team> getTeam(String teamId);
    List<Team> listTeams();
    void sendMessage(String teamId, TeamMessage message);
    List<TeamMessage> getMessages(String teamId);
}
