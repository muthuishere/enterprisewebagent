package com.enterprisewebagent.runtime.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPromptAssemblerTest {

    private PromptSectionRegistry registry;
    private InMemoryPromptSectionCache cache;
    private DefaultPromptAssembler assembler;

    @BeforeEach
    void setUp() {
        registry = new PromptSectionRegistry();
        cache = new InMemoryPromptSectionCache();
        assembler = new DefaultPromptAssembler(registry, cache);

        // Register all static prefix entries
        registry.register(PromptCatalog.SYSTEM_IDENTITY, "You are an AI assistant.");
        registry.register(PromptCatalog.SYSTEM_RULES, "Follow these rules.");
        registry.register(PromptCatalog.DOING_TASKS, "When doing tasks...");
        registry.register(PromptCatalog.ACTION_SAFETY, "Action safety guidelines.");
        registry.register(PromptCatalog.TOOL_USAGE, "Tool usage instructions.");
        registry.register(PromptCatalog.SESSION_GUIDANCE, "Session guidance text.");
        registry.register(PromptCatalog.ASK_USER, "Ask user prompt.");
    }

    @Test
    void defaultPrecedenceAssemblesStaticPrefixBoundaryAndDynamicTail() {
        Map<String, String> dynamic = new LinkedHashMap<>();
        dynamic.put("user_context", "Some user context");

        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.DEFAULT, dynamic,
                null, null, null, null, null, null
        );

        List<PromptSection> sections = assembler.assemble(ctx);

        // Static prefix: 5 entries
        assertEquals("prompt_001_system_identity", sections.get(0).name());
        assertEquals("prompt_002_system_rules", sections.get(1).name());
        assertEquals("prompt_003_doing_tasks", sections.get(2).name());
        assertEquals("prompt_004_action_safety", sections.get(3).name());
        assertEquals("prompt_005_tool_usage", sections.get(4).name());

        // Boundary
        assertEquals("dynamic_boundary", sections.get(5).name());
        assertEquals("---", sections.get(5).content());
        assertFalse(sections.get(5).cached());

        // Dynamic tail: dynamic sections map + SESSION_GUIDANCE + ASK_USER
        assertEquals("user_context", sections.get(6).name());
        assertEquals("Some user context", sections.get(6).content());
        assertEquals("prompt_006_session_guidance", sections.get(7).name());
        assertEquals("prompt_012_ask_user", sections.get(8).name());
    }

    @Test
    void overridePrecedenceReturnsOnlyOverrideNoAppend() {
        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.OVERRIDE, Map.of(),
                "Full override prompt", null, null, null, "Append text", "Memory text"
        );

        List<PromptSection> sections = assembler.assemble(ctx);

        assertEquals(1, sections.size());
        assertEquals("override", sections.get(0).name());
        assertEquals("Full override prompt", sections.get(0).content());
    }

    @Test
    void coordinatorPrecedenceUsesCoordinatorPlusAppend() {
        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.COORDINATOR, Map.of(),
                null, "Coordinator instructions", null, null, "Extra append", null
        );

        List<PromptSection> sections = assembler.assemble(ctx);

        assertEquals(2, sections.size());
        assertEquals("coordinator", sections.get(0).name());
        assertEquals("Coordinator instructions", sections.get(0).content());
        assertEquals("append", sections.get(1).name());
        assertEquals("Extra append", sections.get(1).content());
    }

    @Test
    void cachedSectionsServedFromCacheOnSecondCall() {
        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.DEFAULT, Map.of(),
                null, null, null, null, null, null
        );

        // First call — populates cache
        assembler.assemble(ctx);

        // Verify cache has the static entries
        assertTrue(cache.get(PromptCatalog.SYSTEM_IDENTITY.catalogKey()).isPresent());
        assertTrue(cache.get(PromptCatalog.SYSTEM_RULES.catalogKey()).isPresent());

        // Change registry content — cache should still serve old value
        registry.register(PromptCatalog.SYSTEM_IDENTITY, "CHANGED identity");

        List<PromptSection> sections = assembler.assemble(ctx);
        assertEquals("You are an AI assistant.", sections.get(0).content());
    }

    @Test
    void clearDynamicOnlyClearsUncachedEntries() {
        // Populate cache with both cached and uncached entries
        cache.put(PromptCatalog.SYSTEM_IDENTITY.catalogKey(), "cached identity");
        cache.put(PromptCatalog.SESSION_GUIDANCE.catalogKey(), "dynamic guidance");
        cache.put(PromptCatalog.ASK_USER.catalogKey(), "dynamic ask");

        cache.clearDynamic();

        // Cached entry should remain
        assertTrue(cache.get(PromptCatalog.SYSTEM_IDENTITY.catalogKey()).isPresent());
        // Uncached entries should be gone
        assertFalse(cache.get(PromptCatalog.SESSION_GUIDANCE.catalogKey()).isPresent());
        assertFalse(cache.get(PromptCatalog.ASK_USER.catalogKey()).isPresent());
    }

    @Test
    void orderingIsDeterministic() {
        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.DEFAULT, Map.of(),
                null, null, null, null, "append text", "memory text"
        );

        List<PromptSection> first = assembler.assemble(ctx);
        List<PromptSection> second = assembler.assemble(ctx);

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).name(), second.get(i).name());
            assertEquals(first.get(i).content(), second.get(i).content());
        }
    }

    @Test
    void appendAndMemoryPromptsAreAlwaysLast() {
        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.DEFAULT, Map.of(),
                null, null, null, null, "append instructions", "memory context"
        );

        List<PromptSection> sections = assembler.assemble(ctx);

        int size = sections.size();
        assertTrue(size >= 2);
        assertEquals("memory", sections.get(size - 1).name());
        assertEquals("memory context", sections.get(size - 1).content());
        assertEquals("append", sections.get(size - 2).name());
        assertEquals("append instructions", sections.get(size - 2).content());
    }

    @Test
    void customAgentPrecedenceUsesCustomAgentPrompt() {
        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.CUSTOM_AGENT, Map.of(),
                null, null, "Custom agent prompt", null, null, null
        );

        List<PromptSection> sections = assembler.assemble(ctx);

        assertEquals(1, sections.size());
        assertEquals("custom_agent", sections.get(0).name());
        assertEquals("Custom agent prompt", sections.get(0).content());
    }

    @Test
    void userSystemPrecedenceUsesUserSystemPrompt() {
        PromptContext ctx = new PromptContext(
                "s1", PromptPrecedence.USER_SYSTEM, Map.of(),
                null, null, null, "User system prompt", "Append", null
        );

        List<PromptSection> sections = assembler.assemble(ctx);

        assertEquals(2, sections.size());
        assertEquals("user_system", sections.get(0).name());
        assertEquals("append", sections.get(1).name());
    }

    @Test
    void backwardCompatibleConstructorWorks() {
        PromptContext ctx = new PromptContext("s1", PromptPrecedence.DEFAULT, Map.of());

        assertNull(ctx.overridePrompt());
        assertNull(ctx.appendPrompt());
        assertNull(ctx.memoryPrompt());
    }
}
