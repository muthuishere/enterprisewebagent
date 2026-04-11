package com.enterprisewebagent.runtime.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProviderModelsTest {

    @Test
    void modelsMapHasAllFiveProviders() {
        assertEquals(5, ProviderModels.MODELS.size());
        assertTrue(ProviderModels.MODELS.containsKey("openai"));
        assertTrue(ProviderModels.MODELS.containsKey("anthropic"));
        assertTrue(ProviderModels.MODELS.containsKey("ollama"));
        assertTrue(ProviderModels.MODELS.containsKey("copilot"));
        assertTrue(ProviderModels.MODELS.containsKey("codex"));
    }

    @Test
    void allModelsReturnsFlatList() {
        List<ProviderModels.ModelInfo> all = ProviderModels.allModels();
        int expectedCount = ProviderModels.MODELS.values().stream()
                .mapToInt(List::size)
                .sum();
        assertEquals(expectedCount, all.size());
        // 4 openai + 3 anthropic + 4 ollama + 5 copilot + 4 codex = 20
        assertEquals(20, all.size());
    }

    @Test
    void resolveProviderForOpenai() {
        assertEquals("openai", ProviderModels.resolveProvider("gpt-4.1"));
        assertEquals("openai", ProviderModels.resolveProvider("gpt-4o"));
        assertEquals("openai", ProviderModels.resolveProvider("gpt-4o-mini"));
        assertEquals("openai", ProviderModels.resolveProvider("o3-mini"));
    }

    @Test
    void resolveProviderForAnthropic() {
        assertEquals("anthropic", ProviderModels.resolveProvider("claude-sonnet-4-5-20250929"));
        assertEquals("anthropic", ProviderModels.resolveProvider("claude-opus-4-20250514"));
        assertEquals("anthropic", ProviderModels.resolveProvider("claude-haiku-3-5-20241022"));
    }

    @Test
    void resolveProviderForOllama() {
        assertEquals("ollama", ProviderModels.resolveProvider("ollama:llama3.2"));
        assertEquals("ollama", ProviderModels.resolveProvider("ollama:mistral"));
    }

    @Test
    void resolveProviderForCopilot() {
        assertEquals("copilot", ProviderModels.resolveProvider("copilot:gpt-4.1"));
        assertEquals("copilot", ProviderModels.resolveProvider("copilot:claude-sonnet-4"));
    }

    @Test
    void resolveProviderForCodex() {
        assertEquals("codex", ProviderModels.resolveProvider("codex:gpt-5.4"));
        assertEquals("codex", ProviderModels.resolveProvider("codex:gpt-5.4-mini"));
    }

    @Test
    void resolveProviderReturnsNullForNull() {
        assertNull(ProviderModels.resolveProvider(null));
    }

    @Test
    void resolveModelNameStripsOllamaPrefix() {
        assertEquals("llama3.2", ProviderModels.resolveModelName("ollama:llama3.2"));
        assertEquals("mistral", ProviderModels.resolveModelName("ollama:mistral"));
    }

    @Test
    void resolveModelNameStripsCopilotPrefix() {
        assertEquals("gpt-4.1", ProviderModels.resolveModelName("copilot:gpt-4.1"));
        assertEquals("claude-sonnet-4", ProviderModels.resolveModelName("copilot:claude-sonnet-4"));
    }

    @Test
    void resolveModelNameStripsCodexPrefix() {
        assertEquals("gpt-5.4", ProviderModels.resolveModelName("codex:gpt-5.4"));
        assertEquals("gpt-5.4-mini", ProviderModels.resolveModelName("codex:gpt-5.4-mini"));
    }

    @Test
    void resolveModelNameKeepsPlainNames() {
        assertEquals("gpt-4.1", ProviderModels.resolveModelName("gpt-4.1"));
        assertEquals("claude-sonnet-4-5-20250929", ProviderModels.resolveModelName("claude-sonnet-4-5-20250929"));
    }

    @Test
    void resolveModelNameReturnsNullForNull() {
        assertNull(ProviderModels.resolveModelName(null));
    }

    @Test
    void eachProviderModelHasMatchingProviderField() {
        for (var entry : ProviderModels.MODELS.entrySet()) {
            String providerKey = entry.getKey();
            for (ProviderModels.ModelInfo model : entry.getValue()) {
                assertEquals(providerKey, model.provider(),
                        "Model " + model.id() + " should have provider " + providerKey);
            }
        }
    }
}
