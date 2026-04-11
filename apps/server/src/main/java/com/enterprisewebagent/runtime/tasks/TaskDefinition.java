package com.enterprisewebagent.runtime.tasks;

import java.time.Instant;

public record TaskDefinition(String id, String sessionId, String description, TaskStatus status, Instant created) {
}
