package com.enterprisewebagent.runtime.query;

import java.time.Instant;

public record TranscriptEntry(String role, String content, Instant timestamp) {
}
