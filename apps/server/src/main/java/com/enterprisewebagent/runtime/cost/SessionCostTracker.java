package com.enterprisewebagent.runtime.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCostTracker {

    private final ConcurrentHashMap<String, List<TurnCost>> sessionCosts = new ConcurrentHashMap<>();

    public void recordTurnCost(String sessionId, TurnCost cost) {
        sessionCosts.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(cost);
    }

    public TurnCost getLastTurnCost(String sessionId) {
        List<TurnCost> costs = sessionCosts.get(sessionId);
        if (costs == null || costs.isEmpty()) {
            return null;
        }
        return costs.get(costs.size() - 1);
    }

    public SessionCostSummary getSessionSummary(String sessionId) {
        List<TurnCost> costs = sessionCosts.getOrDefault(sessionId, List.of());
        int totalInput = costs.stream().mapToInt(TurnCost::inputTokens).sum();
        int totalOutput = costs.stream().mapToInt(TurnCost::outputTokens).sum();
        double totalUsd = costs.stream().mapToDouble(TurnCost::totalUsd).sum();
        return new SessionCostSummary(sessionId, costs.size(), totalInput, totalOutput, totalUsd, List.copyOf(costs));
    }

    public double getTotalCost() {
        return sessionCosts.values().stream()
            .flatMap(List::stream)
            .mapToDouble(TurnCost::totalUsd)
            .sum();
    }

    public Map<String, List<TurnCost>> getAllSessions() {
        return Map.copyOf(sessionCosts);
    }
}
