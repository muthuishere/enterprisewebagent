package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.SpringAiModelProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ModelProviderConfigTest {

    @Test
    void stubIsAlwaysAvailableWithNoAiProviders() {
        RuntimeConfig config = new RuntimeConfig();
        DefaultModelProviderRegistry registry = config.modelProviderRegistry("stub", null);

        assertTrue(registry.availableProviders().contains("stub"));
        assertEquals("stub", registry.getDefaultProviderId());
        assertNotNull(registry.getProvider("stub"));
    }

    @Test
    void stubIsAlwaysAvailableWithEmptyProviderList() {
        RuntimeConfig config = new RuntimeConfig();
        DefaultModelProviderRegistry registry = config.modelProviderRegistry("stub", List.of());

        assertTrue(registry.availableProviders().contains("stub"));
        assertEquals("stub", registry.getDefaultProviderId());
    }

    @Test
    void registryReportsAllProvidersIncludingAi() {
        RuntimeConfig config = new RuntimeConfig();
        ChatModel mockModel = mock(ChatModel.class);
        SpringAiModelProvider openai = new SpringAiModelProvider(mockModel, "openai");

        DefaultModelProviderRegistry registry = config.modelProviderRegistry(
                "stub", List.of(openai));

        var providers = registry.availableProviders();
        assertTrue(providers.contains("stub"));
        assertTrue(providers.contains("openai"));
        assertEquals(2, providers.size());
    }

    @Test
    void defaultProviderCanBeSetToAiProvider() {
        RuntimeConfig config = new RuntimeConfig();
        ChatModel mockModel = mock(ChatModel.class);
        SpringAiModelProvider anthropic = new SpringAiModelProvider(mockModel, "anthropic");

        DefaultModelProviderRegistry registry = config.modelProviderRegistry(
                "anthropic", List.of(anthropic));

        assertEquals("anthropic", registry.getDefaultProviderId());
        assertSame(anthropic, registry.getProvider(null));
    }

    @Test
    void unknownDefaultFallsBackToFirstRegistered() {
        RuntimeConfig config = new RuntimeConfig();
        DefaultModelProviderRegistry registry = config.modelProviderRegistry("nonexistent", null);

        assertEquals("stub", registry.getDefaultProviderId(),
                "Should keep stub as default when configured default is not available");
    }
}
