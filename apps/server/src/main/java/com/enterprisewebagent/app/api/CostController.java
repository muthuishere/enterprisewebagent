package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.cost.SessionCostSummary;
import com.enterprisewebagent.runtime.cost.SessionCostTracker;
import com.enterprisewebagent.runtime.cost.TurnCost;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cost")
public class CostController {

    private final SessionCostTracker costTracker;

    public CostController(SessionCostTracker costTracker) {
        this.costTracker = costTracker;
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionCost(@PathVariable String sessionId) {
        SessionCostSummary summary = costTracker.getSessionSummary(sessionId);
        return ResponseEntity.ok(Map.of(
            "sessionId", summary.sessionId(),
            "totalTurns", summary.totalTurns(),
            "totalInputTokens", summary.totalInputTokens(),
            "totalOutputTokens", summary.totalOutputTokens(),
            "totalUsd", summary.totalUsd()
        ));
    }

    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> getTotalCost() {
        return ResponseEntity.ok(Map.of(
            "totalUsd", costTracker.getTotalCost()
        ));
    }
}
