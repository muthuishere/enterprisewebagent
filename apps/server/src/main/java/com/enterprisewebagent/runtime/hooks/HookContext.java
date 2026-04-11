package com.enterprisewebagent.runtime.hooks;

import java.util.Map;

public record HookContext(String sessionId, HookType type, Map<String, Object> data) {
}
