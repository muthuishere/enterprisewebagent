package com.enterprisewebagent.runtime.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultModelProviderRegistryTest {

    private DefaultModelProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultModelProviderRegistry();
    }

    @Test
    void registerAndRetrieve() {
        StubModelProvider provider = new StubModelProvider("hello");
        registry.register("openai", provider);

        assertSame(provider, registry.getProvider("openai"));
    }

    @Test
    void firstRegisteredBecomesDefault() {
        StubModelProvider first = new StubModelProvider("first");
        StubModelProvider second = new StubModelProvider("second");
        registry.register("openai", first);
        registry.register("anthropic", second);

        assertSame(first, registry.getProvider(null));
    }

    @Test
    void unknownProviderThrows() {
        assertThrows(IllegalArgumentException.class, () -> registry.getProvider("nonexistent"));
    }

    @Test
    void nullProviderIdWithNoDefaultThrows() {
        assertThrows(IllegalStateException.class, () -> registry.getProvider(null));
    }

    @Test
    void availableProvidersReturnsAll() {
        registry.register("openai", new StubModelProvider("a"));
        registry.register("anthropic", new StubModelProvider("b"));

        var providers = registry.availableProviders();
        assertEquals(2, providers.size());
        assertTrue(providers.contains("openai"));
        assertTrue(providers.contains("anthropic"));
    }

    @Test
    void setDefaultChangesDefault() {
        StubModelProvider first = new StubModelProvider("first");
        StubModelProvider second = new StubModelProvider("second");
        registry.register("openai", first);
        registry.register("anthropic", second);

        assertSame(first, registry.getProvider(null));

        registry.setDefault("anthropic");
        assertSame(second, registry.getProvider(null));
    }

    @Test
    void setDefaultToUnknownProviderThrows() {
        registry.register("openai", new StubModelProvider("a"));
        assertThrows(IllegalArgumentException.class, () -> registry.setDefault("nonexistent"));
    }
}
