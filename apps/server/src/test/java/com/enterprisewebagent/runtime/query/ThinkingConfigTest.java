package com.enterprisewebagent.runtime.query;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThinkingConfigTest {

    @Test
    void disabled_hasZeroBudget() {
        ThinkingConfig config = ThinkingConfig.DISABLED;
        assertEquals(ThinkingMode.DISABLED, config.mode());
        assertEquals(0, config.budgetTokens());
        assertFalse(config.isEnabled());
    }

    @Test
    void default_hasEnabledWithDefaultBudget() {
        ThinkingConfig config = ThinkingConfig.DEFAULT;
        assertEquals(ThinkingMode.ENABLED, config.mode());
        assertEquals(4096, config.budgetTokens());
        assertTrue(config.isEnabled());
    }

    @Test
    void enabled_enforcesMinBudget() {
        ThinkingConfig config = new ThinkingConfig(ThinkingMode.ENABLED, 100);
        assertEquals(256, config.budgetTokens()); // clamped to min
    }

    @Test
    void enabled_enforcesMaxBudget() {
        ThinkingConfig config = new ThinkingConfig(ThinkingMode.ENABLED, 100000);
        assertEquals(32768, config.budgetTokens()); // clamped to max
    }

    @Test
    void negativeBudget_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThinkingConfig(ThinkingMode.ENABLED, -1));
    }

    @Test
    void adaptive_isEnabled() {
        ThinkingConfig config = new ThinkingConfig(ThinkingMode.ADAPTIVE);
        assertTrue(config.isEnabled());
    }

    @Test
    void fromOptions_disabledByDefault() {
        ThinkingConfig config = ThinkingConfig.fromOptions(Map.of());
        assertEquals(ThinkingMode.DISABLED, config.mode());
    }

    @Test
    void fromOptions_nullOptions() {
        ThinkingConfig config = ThinkingConfig.fromOptions(null);
        assertEquals(ThinkingMode.DISABLED, config.mode());
    }

    @Test
    void fromOptions_parsesEnabledWithBudget() {
        ThinkingConfig config = ThinkingConfig.fromOptions(Map.of(
                "thinking_mode", "enabled",
                "thinking_budget", 8192
        ));
        assertEquals(ThinkingMode.ENABLED, config.mode());
        assertEquals(8192, config.budgetTokens());
    }

    @Test
    void fromOptions_parsesAdaptive() {
        ThinkingConfig config = ThinkingConfig.fromOptions(Map.of(
                "thinking_mode", "adaptive"
        ));
        assertEquals(ThinkingMode.ADAPTIVE, config.mode());
    }

    @Test
    void fromOptions_handlesStringBudget() {
        ThinkingConfig config = ThinkingConfig.fromOptions(Map.of(
                "thinking_mode", "enabled",
                "thinking_budget", "2048"
        ));
        assertEquals(2048, config.budgetTokens());
    }

    @Test
    void fromOptions_invalidMode_defaultsToDisabled() {
        ThinkingConfig config = ThinkingConfig.fromOptions(Map.of(
                "thinking_mode", "invalid_mode"
        ));
        assertEquals(ThinkingMode.DISABLED, config.mode());
    }

    @Test
    void fromOptions_invalidBudgetString_usesDefault() {
        ThinkingConfig config = ThinkingConfig.fromOptions(Map.of(
                "thinking_mode", "enabled",
                "thinking_budget", "not_a_number"
        ));
        assertEquals(4096, config.budgetTokens());
    }
}
