package com.enterprisewebagent.runtime.cost;

import java.util.Map;

public final class ModelPricing {

    private ModelPricing() {}

    public record Price(double inputPerMillion, double outputPerMillion, double cacheReadPerMillion) {
        public Price(double inputPerMillion, double outputPerMillion) {
            this(inputPerMillion, outputPerMillion, 0.0);
        }
    }

    private static final Price FREE = new Price(0.0, 0.0, 0.0);

    private static final Map<String, Price> PRICES = Map.ofEntries(
        Map.entry("gpt-4.1", new Price(2.00, 8.00)),
        Map.entry("gpt-4o", new Price(2.50, 10.00)),
        Map.entry("gpt-4o-mini", new Price(0.15, 0.60)),
        Map.entry("claude-sonnet-4-20250514", new Price(3.00, 15.00)),
        Map.entry("claude-opus-4-20250514", new Price(15.00, 75.00)),
        Map.entry("claude-3-5-haiku-20241022", new Price(1.00, 5.00))
    );

    private static final Price DEFAULT_PRICE = new Price(2.50, 10.00);

    public static Price getPrice(String model) {
        if (model == null || model.isBlank()) {
            return DEFAULT_PRICE;
        }

        String lower = model.toLowerCase();

        // Free models
        if (lower.startsWith("ollama") || lower.contains("ollama")) {
            return FREE;
        }
        if (lower.startsWith("copilot") || lower.contains("copilot")) {
            return FREE;
        }
        if (lower.startsWith("codex") || lower.contains("codex")) {
            return FREE;
        }
        if ("stub".equals(lower)) {
            return FREE;
        }

        // Exact match
        Price exact = PRICES.get(model);
        if (exact != null) {
            return exact;
        }

        // Prefix match for versioned models
        for (Map.Entry<String, Price> entry : PRICES.entrySet()) {
            if (lower.startsWith(entry.getKey()) || lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return DEFAULT_PRICE;
    }

    public static boolean isFreeModel(String model) {
        Price price = getPrice(model);
        return price.inputPerMillion() == 0.0 && price.outputPerMillion() == 0.0;
    }
}
