package com.enterprisewebagent.runtime.cost;

import java.time.Duration;

public record TurnCost(
    String model,
    int inputTokens,
    int outputTokens,
    int cacheTokens,
    double totalUsd,
    Duration apiDuration
) {}
