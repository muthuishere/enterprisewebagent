package com.enterprisewebagent.runtime.query;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptBudgetCalculatorTest {

    private static PromptSection section(String content) {
        return new PromptSection("test", content, false);
    }

    private static TranscriptEntry entry(String content) {
        return new TranscriptEntry("user", content, Instant.now());
    }

    @Test
    void estimateTokensReturnsReasonableEstimate() {
        // 400 chars / 4 = 100 tokens
        var sections = List.of(section("a".repeat(400)));
        assertEquals(100, PromptBudgetCalculator.estimateTokens(sections));
    }

    @Test
    void estimateTokensMultipleSections() {
        var sections = List.of(section("a".repeat(40)), section("b".repeat(80)));
        // 40/4 + 80/4 = 10 + 20 = 30
        assertEquals(30, PromptBudgetCalculator.estimateTokens(sections));
    }

    @Test
    void checkBudgetWithinLimitsReturnsOk() {
        var sections = List.of(section("a".repeat(100)));
        var transcript = List.of(entry("b".repeat(100)));
        var result = PromptBudgetCalculator.checkBudget(sections, transcript);
        assertTrue(result.withinBudget());
        assertEquals("OK", result.recommendation());
    }

    @Test
    void checkBudgetOverLimitReturnsNotWithinBudget() {
        // Need > 100_000 tokens -> > 400_000 chars
        var sections = List.of(section("a".repeat(200_000)));
        var transcript = List.of(entry("b".repeat(250_000)));
        var result = PromptBudgetCalculator.checkBudget(sections, transcript);
        assertFalse(result.withinBudget());
        assertTrue(result.recommendation().startsWith("PROMPT_TOO_LONG"));
    }

    @Test
    void checkBudgetApproachingLimitGivesWarning() {
        // Need 80_000-100_000 tokens -> 320_000-400_000 chars
        // 340_000 chars / 4 = 85_000 tokens -> approaching
        var sections = List.of(section("a".repeat(340_000)));
        var transcript = List.<TranscriptEntry>of();
        var result = PromptBudgetCalculator.checkBudget(sections, transcript);
        assertTrue(result.withinBudget());
        assertTrue(result.recommendation().startsWith("APPROACHING_LIMIT"));
    }

    @Test
    void checkOutputBudgetWithinLimit() {
        var result = PromptBudgetCalculator.checkOutputBudget(5000);
        assertTrue(result.withinBudget());
        assertEquals("OK", result.recommendation());
    }

    @Test
    void checkOutputBudgetOverLimit() {
        var result = PromptBudgetCalculator.checkOutputBudget(10_000);
        assertFalse(result.withinBudget());
        assertTrue(result.recommendation().startsWith("OUTPUT_TOO_LONG"));
    }

    @Test
    void estimateTranscriptTokens() {
        var entries = List.of(entry("a".repeat(80)), entry("b".repeat(120)));
        // 80/4 + 120/4 = 20 + 30 = 50
        assertEquals(50, PromptBudgetCalculator.estimateTranscriptTokens(entries));
    }
}
