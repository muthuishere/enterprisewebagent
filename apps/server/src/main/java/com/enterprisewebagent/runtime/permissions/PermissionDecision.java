package com.enterprisewebagent.runtime.permissions;

public record PermissionDecision(boolean allowed, String reason, boolean requiresApproval) {}
