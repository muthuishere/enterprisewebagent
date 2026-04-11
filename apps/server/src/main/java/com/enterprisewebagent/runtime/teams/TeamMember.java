package com.enterprisewebagent.runtime.teams;

import java.util.Map;

public record TeamMember(String agentId, String role, String model, Map<String, String> config) {
}
