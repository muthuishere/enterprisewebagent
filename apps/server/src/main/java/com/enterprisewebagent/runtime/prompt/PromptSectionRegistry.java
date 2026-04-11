package com.enterprisewebagent.runtime.prompt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PromptSectionRegistry {

    private final Map<PromptCatalog, String> entries = new ConcurrentHashMap<>();

    public void register(PromptCatalog entry, String content) {
        entries.put(entry, content);
    }

    public Optional<String> getContent(PromptCatalog entry) {
        return Optional.ofNullable(entries.get(entry));
    }

    public List<PromptCatalog> registeredEntries() {
        return List.copyOf(entries.keySet());
    }
}
