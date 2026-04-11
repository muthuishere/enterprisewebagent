package com.enterprisewebagent.runtime.cost;

import java.time.Duration;

public class CostCalculator {

    public TurnCost calculate(String model, int inputTokens, int outputTokens, int cacheTokens, Duration apiDuration) {
        ModelPricing.Price price = ModelPricing.getPrice(model);
        double cost = (inputTokens * price.inputPerMillion() / 1_000_000.0)
                    + (outputTokens * price.outputPerMillion() / 1_000_000.0)
                    + (cacheTokens * price.cacheReadPerMillion() / 1_000_000.0);
        return new TurnCost(model, inputTokens, outputTokens, cacheTokens, cost, apiDuration);
    }
}
