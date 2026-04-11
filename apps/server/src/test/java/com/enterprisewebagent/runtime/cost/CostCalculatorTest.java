package com.enterprisewebagent.runtime.cost;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CostCalculatorTest {

    private CostCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CostCalculator();
    }

    @Test
    void calculateGpt4oCost() {
        TurnCost cost = calculator.calculate("gpt-4o", 1_000_000, 1_000_000, 0, Duration.ofSeconds(5));
        // input: 2.50, output: 10.00 = 12.50
        assertEquals(12.50, cost.totalUsd(), 0.001);
        assertEquals("gpt-4o", cost.model());
        assertEquals(1_000_000, cost.inputTokens());
        assertEquals(1_000_000, cost.outputTokens());
    }

    @Test
    void calculateGpt4oMiniCost() {
        TurnCost cost = calculator.calculate("gpt-4o-mini", 1_000_000, 1_000_000, 0, Duration.ofSeconds(3));
        // input: 0.15, output: 0.60 = 0.75
        assertEquals(0.75, cost.totalUsd(), 0.001);
    }

    @Test
    void calculateSmallTokenCount() {
        TurnCost cost = calculator.calculate("gpt-4o", 1000, 500, 0, Duration.ofMillis(500));
        // input: 1000 * 2.50 / 1M = 0.0025, output: 500 * 10.00 / 1M = 0.005
        assertEquals(0.0075, cost.totalUsd(), 0.0001);
    }

    @Test
    void calculateFreeModel() {
        TurnCost cost = calculator.calculate("ollama:llama3", 10000, 5000, 0, Duration.ofSeconds(2));
        assertEquals(0.0, cost.totalUsd());
    }

    @Test
    void calculateWithCacheTokens() {
        // CacheReadPerMillion is 0 for standard prices, but test the calculation path
        TurnCost cost = calculator.calculate("gpt-4o", 500, 200, 100, Duration.ofSeconds(1));
        double expected = (500 * 2.50 / 1_000_000.0) + (200 * 10.00 / 1_000_000.0);
        assertEquals(expected, cost.totalUsd(), 0.00001);
    }

    @Test
    void calculateClaudeOpusCost() {
        TurnCost cost = calculator.calculate("claude-opus-4-20250514", 1_000_000, 1_000_000, 0, Duration.ofSeconds(10));
        // input: 15.00, output: 75.00 = 90.00
        assertEquals(90.00, cost.totalUsd(), 0.001);
    }

    @Test
    void durationIsPreserved() {
        Duration duration = Duration.ofMillis(1234);
        TurnCost cost = calculator.calculate("gpt-4o", 100, 50, 0, duration);
        assertEquals(duration, cost.apiDuration());
    }

    @Test
    void zeroTokensZeroCost() {
        TurnCost cost = calculator.calculate("gpt-4o", 0, 0, 0, Duration.ZERO);
        assertEquals(0.0, cost.totalUsd());
    }
}
