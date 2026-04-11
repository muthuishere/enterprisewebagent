package com.enterprisewebagent.runtime.query;

public record ThinkingConfig(ThinkingMode mode, int budgetTokens) {

    private static final int DEFAULT_BUDGET = 4096;
    private static final int MIN_BUDGET = 256;
    private static final int MAX_BUDGET = 32768;

    public static final ThinkingConfig DISABLED = new ThinkingConfig(ThinkingMode.DISABLED, 0);
    public static final ThinkingConfig DEFAULT = new ThinkingConfig(ThinkingMode.ENABLED, DEFAULT_BUDGET);

    public ThinkingConfig {
        if (budgetTokens < 0) {
            throw new IllegalArgumentException("budgetTokens must be non-negative, got: " + budgetTokens);
        }
        if (mode == ThinkingMode.ENABLED && budgetTokens < MIN_BUDGET) {
            budgetTokens = MIN_BUDGET;
        }
        if (budgetTokens > MAX_BUDGET) {
            budgetTokens = MAX_BUDGET;
        }
    }

    public ThinkingConfig(ThinkingMode mode) {
        this(mode, mode == ThinkingMode.DISABLED ? 0 : DEFAULT_BUDGET);
    }

    public boolean isEnabled() {
        return mode != ThinkingMode.DISABLED;
    }

    public static ThinkingConfig fromOptions(java.util.Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return DISABLED;
        }
        String modeStr = String.valueOf(options.getOrDefault("thinking_mode", "disabled"));
        ThinkingMode mode;
        try {
            mode = ThinkingMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            mode = ThinkingMode.DISABLED;
        }
        int budget = DEFAULT_BUDGET;
        Object budgetObj = options.get("thinking_budget");
        if (budgetObj instanceof Number n) {
            budget = n.intValue();
        } else if (budgetObj instanceof String s) {
            try {
                budget = Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return new ThinkingConfig(mode, budget);
    }
}
