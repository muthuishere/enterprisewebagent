package com.enterprisewebagent.runtime.hooks;

public record Hook(String id, HookType type, String command, int order, boolean enabled) {
}
