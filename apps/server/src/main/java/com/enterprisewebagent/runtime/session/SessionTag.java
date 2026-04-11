package com.enterprisewebagent.runtime.session;

import java.time.Instant;

public record SessionTag(String sessionId, String tag, Instant created) {
}
