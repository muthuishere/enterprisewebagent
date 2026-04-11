package com.enterprisewebagent.runtime.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelRegistryTest {

    private DefaultModelProviderRegistry providerRegistry;

    @BeforeEach
    void setUp() {
        providerRegistry = new DefaultModelProviderRegistry();
    }

    @Test
    void refreshPopulatesModels() {
        providerRegistry.register("openai", new StubModelProvider("ok"));

        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.AvailableModel> models = registry.getAvailableModels();
        assertFalse(models.isEmpty());
        // Should have all 20 catalog models
        assertEquals(20, models.size());
    }

    @Test
    void providerStatusDetectsRegistered() {
        providerRegistry.register("openai", new StubModelProvider("ok"));
        providerRegistry.register("anthropic", new StubModelProvider("ok"));

        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.ProviderStatus> statuses = registry.getProviderStatuses();
        ModelRegistry.ProviderStatus openai = statuses.stream()
                .filter(s -> s.provider().equals("openai")).findFirst().orElseThrow();
        ModelRegistry.ProviderStatus anthropic = statuses.stream()
                .filter(s -> s.provider().equals("anthropic")).findFirst().orElseThrow();

        assertTrue(openai.available());
        assertTrue(anthropic.available());
    }

    @Test
    void unavailableProvidersMarkedCorrectly() {
        // No providers registered
        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.ProviderStatus> statuses = registry.getProviderStatuses();
        ModelRegistry.ProviderStatus openai = statuses.stream()
                .filter(s -> s.provider().equals("openai")).findFirst().orElseThrow();
        ModelRegistry.ProviderStatus anthropic = statuses.stream()
                .filter(s -> s.provider().equals("anthropic")).findFirst().orElseThrow();

        assertFalse(openai.available());
        assertFalse(anthropic.available());
        assertNotNull(openai.reason());
        assertNotNull(anthropic.reason());
    }

    @Test
    void availableModelsReflectProviderStatus() {
        providerRegistry.register("openai", new StubModelProvider("ok"));
        // anthropic NOT registered

        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.AvailableModel> models = registry.getAvailableModels();

        // OpenAI models should be available
        List<ModelRegistry.AvailableModel> openaiModels = models.stream()
                .filter(m -> m.provider().equals("openai")).toList();
        assertFalse(openaiModels.isEmpty());
        assertTrue(openaiModels.stream().allMatch(ModelRegistry.AvailableModel::available));

        // Anthropic models should not be available
        List<ModelRegistry.AvailableModel> anthropicModels = models.stream()
                .filter(m -> m.provider().equals("anthropic")).toList();
        assertFalse(anthropicModels.isEmpty());
        assertTrue(anthropicModels.stream().noneMatch(ModelRegistry.AvailableModel::available));
    }

    @Test
    void refreshCanBeCalledMultipleTimes() {
        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.AvailableModel> first = registry.getAvailableModels();

        // Register a provider then refresh again
        providerRegistry.register("openai", new StubModelProvider("ok"));
        registry.refresh();

        List<ModelRegistry.AvailableModel> second = registry.getAvailableModels();

        // First call: openai unavailable. Second call: openai available.
        long firstAvailableOpenai = first.stream()
                .filter(m -> m.provider().equals("openai") && m.available()).count();
        long secondAvailableOpenai = second.stream()
                .filter(m -> m.provider().equals("openai") && m.available()).count();

        assertEquals(0, firstAvailableOpenai);
        assertTrue(secondAvailableOpenai > 0);
    }

    @Test
    void ollamaNotReachableMarkedUnavailable() {
        // Use an unreachable URL so the health check fails fast
        ModelRegistry registry = new ModelRegistry(providerRegistry, "http://127.0.0.1:1");
        registry.refresh();

        ModelRegistry.ProviderStatus ollama = registry.getProviderStatuses().stream()
                .filter(s -> s.provider().equals("ollama")).findFirst().orElseThrow();
        assertFalse(ollama.available());
        assertNotNull(ollama.reason());
    }

    @Test
    void providerStatusesHasAllFiveProviders() {
        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.ProviderStatus> statuses = registry.getProviderStatuses();
        assertEquals(5, statuses.size());
        assertTrue(statuses.stream().anyMatch(s -> s.provider().equals("openai")));
        assertTrue(statuses.stream().anyMatch(s -> s.provider().equals("anthropic")));
        assertTrue(statuses.stream().anyMatch(s -> s.provider().equals("ollama")));
        assertTrue(statuses.stream().anyMatch(s -> s.provider().equals("copilot")));
        assertTrue(statuses.stream().anyMatch(s -> s.provider().equals("codex")));
    }

    @Test
    void getAvailableModelsReturnsImmutableCopy() {
        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.AvailableModel> models = registry.getAvailableModels();
        assertThrows(UnsupportedOperationException.class, () ->
                models.add(new ModelRegistry.AvailableModel("test", "Test", "test", true)));
    }

    @Test
    void getProviderStatusesReturnsImmutableCopy() {
        ModelRegistry registry = new ModelRegistry(providerRegistry);
        registry.refresh();

        List<ModelRegistry.ProviderStatus> statuses = registry.getProviderStatuses();
        assertThrows(UnsupportedOperationException.class, () ->
                statuses.add(new ModelRegistry.ProviderStatus("test", true, "test")));
    }
}
