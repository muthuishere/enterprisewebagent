package com.enterprisewebagent.runtime.agents.custom;

import java.util.List;
import java.util.Map;

public record CustomAgentDefinition(
    String name,
    String description,
    String systemPrompt,
    String model,
    List<String> allowedTools,
    Map<String, String> config
) {
}
