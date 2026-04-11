package com.enterprisewebagent.runtime.prompt;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryPromptSectionCache implements PromptSectionCache {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String name) {
        return Optional.ofNullable(store.get(name));
    }

    @Override
    public void put(String name, String content) {
        store.put(name, content);
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public void clearDynamic() {
        Set<String> dynamicKeys = Set.of(
                java.util.Arrays.stream(PromptCatalog.values())
                        .filter(e -> !e.cached())
                        .map(PromptCatalog::catalogKey)
                        .toArray(String[]::new)
        );
        store.keySet().removeIf(dynamicKeys::contains);
    }
}
