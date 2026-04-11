package com.enterprisewebagent.runtime.cost;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelPricingTest {

    @Test
    void gpt41HasCorrectPrice() {
        ModelPricing.Price price = ModelPricing.getPrice("gpt-4.1");
        assertEquals(2.00, price.inputPerMillion(), 0.001);
        assertEquals(8.00, price.outputPerMillion(), 0.001);
    }

    @Test
    void gpt4oHasCorrectPrice() {
        ModelPricing.Price price = ModelPricing.getPrice("gpt-4o");
        assertEquals(2.50, price.inputPerMillion(), 0.001);
        assertEquals(10.00, price.outputPerMillion(), 0.001);
    }

    @Test
    void gpt4oMiniHasCorrectPrice() {
        ModelPricing.Price price = ModelPricing.getPrice("gpt-4o-mini");
        assertEquals(0.15, price.inputPerMillion(), 0.001);
        assertEquals(0.60, price.outputPerMillion(), 0.001);
    }

    @Test
    void claudeSonnetHasCorrectPrice() {
        ModelPricing.Price price = ModelPricing.getPrice("claude-sonnet-4-20250514");
        assertEquals(3.00, price.inputPerMillion(), 0.001);
        assertEquals(15.00, price.outputPerMillion(), 0.001);
    }

    @Test
    void claudeOpusHasCorrectPrice() {
        ModelPricing.Price price = ModelPricing.getPrice("claude-opus-4-20250514");
        assertEquals(15.00, price.inputPerMillion(), 0.001);
        assertEquals(75.00, price.outputPerMillion(), 0.001);
    }

    @Test
    void claudeHaikuHasCorrectPrice() {
        ModelPricing.Price price = ModelPricing.getPrice("claude-3-5-haiku-20241022");
        assertEquals(1.00, price.inputPerMillion(), 0.001);
        assertEquals(5.00, price.outputPerMillion(), 0.001);
    }

    @Test
    void ollamaModelIsFree() {
        ModelPricing.Price price = ModelPricing.getPrice("ollama:llama3");
        assertEquals(0.0, price.inputPerMillion());
        assertEquals(0.0, price.outputPerMillion());
        assertTrue(ModelPricing.isFreeModel("ollama:llama3"));
    }

    @Test
    void copilotModelIsFree() {
        ModelPricing.Price price = ModelPricing.getPrice("copilot-gpt4");
        assertEquals(0.0, price.inputPerMillion());
        assertEquals(0.0, price.outputPerMillion());
        assertTrue(ModelPricing.isFreeModel("copilot-gpt4"));
    }

    @Test
    void codexModelIsFree() {
        ModelPricing.Price price = ModelPricing.getPrice("codex-mini");
        assertEquals(0.0, price.inputPerMillion());
        assertEquals(0.0, price.outputPerMillion());
        assertTrue(ModelPricing.isFreeModel("codex-mini"));
    }

    @Test
    void stubModelIsFree() {
        assertTrue(ModelPricing.isFreeModel("stub"));
    }

    @Test
    void unknownModelReturnsDefault() {
        ModelPricing.Price price = ModelPricing.getPrice("some-unknown-model");
        assertEquals(2.50, price.inputPerMillion(), 0.001);
        assertEquals(10.00, price.outputPerMillion(), 0.001);
    }

    @Test
    void nullModelReturnsDefault() {
        ModelPricing.Price price = ModelPricing.getPrice(null);
        assertEquals(2.50, price.inputPerMillion(), 0.001);
    }

    @Test
    void emptyModelReturnsDefault() {
        ModelPricing.Price price = ModelPricing.getPrice("");
        assertEquals(2.50, price.inputPerMillion(), 0.001);
    }

    @Test
    void isFreeModelReturnsFalseForPaid() {
        assertFalse(ModelPricing.isFreeModel("gpt-4o"));
    }
}
