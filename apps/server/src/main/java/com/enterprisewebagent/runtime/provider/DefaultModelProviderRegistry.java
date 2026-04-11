package com.enterprisewebagent.runtime.provider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultModelProviderRegistry implements ModelProviderRegistry {

    private final Map<String, ModelProvider> providers = new ConcurrentHashMap<>();
    private volatile String defaultProviderId;

    public void register(String providerId, ModelProvider provider) {
        providers.put(providerId, provider);
        if (defaultProviderId == null) {
            defaultProviderId = providerId;
        }
    }

    @Override
    public ModelProvider getProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            if (defaultProviderId == null) {
                throw new IllegalStateException("No providers registered");
            }
            return providers.get(defaultProviderId);
        }
        ModelProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerId);
        }
        return provider;
    }

    @Override
    public List<String> availableProviders() {
        return List.copyOf(providers.keySet());
    }

    public void setDefault(String providerId) {
        if (!providers.containsKey(providerId)) {
            throw new IllegalArgumentException("Unknown provider: " + providerId);
        }
        this.defaultProviderId = providerId;
    }
}
