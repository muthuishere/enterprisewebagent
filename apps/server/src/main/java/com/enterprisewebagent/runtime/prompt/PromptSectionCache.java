package com.enterprisewebagent.runtime.prompt;

import java.util.Optional;

public interface PromptSectionCache {
    Optional<String> get(String name);
    void put(String name, String content);
    void clear();
    void clearDynamic();
}
