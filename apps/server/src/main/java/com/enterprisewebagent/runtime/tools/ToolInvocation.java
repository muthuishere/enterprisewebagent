package com.enterprisewebagent.runtime.tools;

import java.util.Map;

public record ToolInvocation(String name, Map<String, Object> arguments) {
}
