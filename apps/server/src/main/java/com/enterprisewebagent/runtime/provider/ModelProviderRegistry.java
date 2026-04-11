package com.enterprisewebagent.runtime.provider;

import java.util.List;

public interface ModelProviderRegistry {
    ModelProvider getProvider(String providerId);
    List<String> availableProviders();
}
