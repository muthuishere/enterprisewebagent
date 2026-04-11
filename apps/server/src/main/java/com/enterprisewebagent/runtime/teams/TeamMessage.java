package com.enterprisewebagent.runtime.teams;

import java.time.Instant;

public record TeamMessage(String from, String to, String content, Instant timestamp) {
}
