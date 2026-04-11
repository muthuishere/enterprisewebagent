package com.enterprisewebagent.runtime.permissions;

public record PermissionRule(
    String id,
    PermissionType type,
    PermissionScope scope,
    String pattern,
    String description
) {}
