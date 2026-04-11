package com.enterprisewebagent.runtime.session;

import com.enterprisewebagent.runtime.query.TranscriptEntry;

import java.time.Instant;
import java.util.List;

public record Session(String id, String workspaceId, Instant created, Instant lastActive, SessionStatus status, List<TranscriptEntry> transcript) {
}
