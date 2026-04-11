package com.enterprisewebagent.runtime.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultPromptAssembler implements PromptAssembler {

    private static final List<PromptCatalog> STATIC_PREFIX = List.of(
            PromptCatalog.SYSTEM_IDENTITY,
            PromptCatalog.SYSTEM_RULES,
            PromptCatalog.DOING_TASKS,
            PromptCatalog.ACTION_SAFETY,
            PromptCatalog.TOOL_USAGE
    );

    private static final PromptSection DYNAMIC_BOUNDARY =
            new PromptSection("dynamic_boundary", "---", false);

    private final PromptSectionRegistry registry;
    private final PromptSectionCache cache;

    public DefaultPromptAssembler(PromptSectionRegistry registry, PromptSectionCache cache) {
        this.registry = registry;
        this.cache = cache;
    }

    @Override
    public List<PromptSection> assemble(PromptContext context) {
        List<PromptSection> sections = new ArrayList<>();

        switch (context.activePrecedence()) {
            case OVERRIDE -> {
                if (context.overridePrompt() != null) {
                    sections.add(new PromptSection("override", context.overridePrompt(), false));
                }
                // OVERRIDE skips append and memory
                return sections;
            }
            case COORDINATOR -> {
                if (context.coordinatorPrompt() != null) {
                    sections.add(new PromptSection("coordinator", context.coordinatorPrompt(), false));
                }
            }
            case CUSTOM_AGENT -> {
                if (context.customAgentPrompt() != null) {
                    sections.add(new PromptSection("custom_agent", context.customAgentPrompt(), false));
                }
            }
            case USER_SYSTEM -> {
                if (context.userSystemPrompt() != null) {
                    sections.add(new PromptSection("user_system", context.userSystemPrompt(), false));
                }
            }
            case DEFAULT -> {
                // Static prefix sections — use cache
                for (PromptCatalog entry : STATIC_PREFIX) {
                    resolveSection(entry).ifPresent(sections::add);
                }

                sections.add(DYNAMIC_BOUNDARY);

                // Dynamic tail: dynamic sections map, then SESSION_GUIDANCE, then ASK_USER
                Map<String, String> dynamic = context.dynamicSections();
                if (dynamic != null) {
                    dynamic.forEach((name, content) ->
                            sections.add(new PromptSection(name, content, false)));
                }
                resolveSection(PromptCatalog.SESSION_GUIDANCE).ifPresent(sections::add);
                resolveSection(PromptCatalog.ASK_USER).ifPresent(sections::add);
            }
        }

        // Append prompt (unless OVERRIDE — already returned above)
        if (context.appendPrompt() != null) {
            sections.add(new PromptSection("append", context.appendPrompt(), false));
        }

        // Memory prompt
        if (context.memoryPrompt() != null) {
            sections.add(new PromptSection("memory", context.memoryPrompt(), false));
        }

        return sections;
    }

    private java.util.Optional<PromptSection> resolveSection(PromptCatalog entry) {
        if (entry.cached()) {
            // Check cache first
            java.util.Optional<String> cached = cache.get(entry.catalogKey());
            if (cached.isPresent()) {
                return java.util.Optional.of(entry.toSection(cached.get()));
            }
            // Cache miss — resolve from registry and cache
            return registry.getContent(entry).map(content -> {
                cache.put(entry.catalogKey(), content);
                return entry.toSection(content);
            });
        } else {
            // Uncached — always resolve fresh from registry, never cache
            return registry.getContent(entry).map(entry::toSection);
        }
    }
}
