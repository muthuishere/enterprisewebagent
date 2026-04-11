package com.enterprisewebagent.runtime.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPromptSectionCacheTest {

    private InMemoryPromptSectionCache cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryPromptSectionCache();
    }

    @Test
    void putAndGet() {
        cache.put("key1", "value1");

        assertTrue(cache.get("key1").isPresent());
        assertEquals("value1", cache.get("key1").get());
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertTrue(cache.get("nonexistent").isEmpty());
    }

    @Test
    void clearRemovesEverything() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put(PromptCatalog.SYSTEM_IDENTITY.catalogKey(), "identity");
        cache.put(PromptCatalog.SESSION_GUIDANCE.catalogKey(), "guidance");

        cache.clear();

        assertTrue(cache.get("key1").isEmpty());
        assertTrue(cache.get("key2").isEmpty());
        assertTrue(cache.get(PromptCatalog.SYSTEM_IDENTITY.catalogKey()).isEmpty());
        assertTrue(cache.get(PromptCatalog.SESSION_GUIDANCE.catalogKey()).isEmpty());
    }

    @Test
    void clearDynamicOnlyClearsUncachedCatalogEntries() {
        // Cached catalog entries (should survive clearDynamic)
        cache.put(PromptCatalog.SYSTEM_IDENTITY.catalogKey(), "identity");
        cache.put(PromptCatalog.SYSTEM_RULES.catalogKey(), "rules");
        cache.put(PromptCatalog.TOOL_USAGE.catalogKey(), "tools");

        // Uncached catalog entries (should be cleared)
        cache.put(PromptCatalog.SESSION_GUIDANCE.catalogKey(), "guidance");
        cache.put(PromptCatalog.ASK_USER.catalogKey(), "ask");

        // Non-catalog keys (should survive — not in the dynamic catalog set)
        cache.put("custom_key", "custom_value");

        cache.clearDynamic();

        // Cached entries survive
        assertTrue(cache.get(PromptCatalog.SYSTEM_IDENTITY.catalogKey()).isPresent());
        assertTrue(cache.get(PromptCatalog.SYSTEM_RULES.catalogKey()).isPresent());
        assertTrue(cache.get(PromptCatalog.TOOL_USAGE.catalogKey()).isPresent());

        // Uncached entries cleared
        assertTrue(cache.get(PromptCatalog.SESSION_GUIDANCE.catalogKey()).isEmpty());
        assertTrue(cache.get(PromptCatalog.ASK_USER.catalogKey()).isEmpty());

        // Non-catalog keys survive
        assertTrue(cache.get("custom_key").isPresent());
    }

    @Test
    void putOverwritesExistingValue() {
        cache.put("key1", "original");
        cache.put("key1", "updated");

        assertEquals("updated", cache.get("key1").get());
    }
}
