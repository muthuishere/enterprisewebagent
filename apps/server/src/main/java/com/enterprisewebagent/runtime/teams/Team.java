package com.enterprisewebagent.runtime.teams;

import java.time.Instant;
import java.util.List;

public record Team(String id, String name, List<TeamMember> members, Instant created) {
}
