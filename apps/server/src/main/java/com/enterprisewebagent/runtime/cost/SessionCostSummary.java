package com.enterprisewebagent.runtime.cost;

import java.util.List;

public record SessionCostSummary(
    String sessionId,
    int totalTurns,
    int totalInputTokens,
    int totalOutputTokens,
    double totalUsd,
    List<TurnCost> turns
) {}
