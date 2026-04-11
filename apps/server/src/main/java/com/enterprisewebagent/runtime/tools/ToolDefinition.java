package com.enterprisewebagent.runtime.tools;

import java.util.Map;

public record ToolDefinition(String name, String description, Map<String, Object> parameters, boolean builtIn) {
}
