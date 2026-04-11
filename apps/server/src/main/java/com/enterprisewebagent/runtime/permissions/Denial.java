package com.enterprisewebagent.runtime.permissions;

import java.time.Instant;

public record Denial(String toolName, String reason, Instant timestamp) {}
