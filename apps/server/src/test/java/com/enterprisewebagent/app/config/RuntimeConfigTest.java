package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.agents.WorkerOrchestrator;
import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptSectionCache;
import com.enterprisewebagent.runtime.prompt.PromptSectionRegistry;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.SpringAiModelProvider;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.app.persistence.SessionRepository;
import com.enterprisewebagent.app.persistence.TaskRepository;
import com.enterprisewebagent.app.persistence.TranscriptEntryRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RuntimeConfigTest {

    private final RuntimeConfig config = new RuntimeConfig();

    @Test
    void allBeansAreNonNull() {
        PromptSectionCache cache = config.promptSectionCache();
        PromptSectionRegistry registry = config.promptSectionRegistry();
        PromptAssembler assembler = config.promptAssembler(registry, cache);
        InMemoryEventPublisher eventPublisher = config.eventPublisher();
        DefaultToolRegistry toolRegistry = config.toolRegistry(eventPublisher);
        SessionManager sessionManager = config.sessionManager(
                mock(SessionRepository.class), mock(TranscriptEntryRepository.class));
        TaskManager taskManager = config.taskManager(mock(TaskRepository.class));
        DefaultModelProviderRegistry providerRegistry = config.modelProviderRegistry("stub", null);
        RuntimeMetrics runtimeMetrics = new RuntimeMetrics(new SimpleMeterRegistry());
        TurnEngine turnEngine = config.turnEngine(providerRegistry, toolRegistry, eventPublisher, runtimeMetrics);
        WorkerOrchestrator orchestrator = config.workerOrchestrator(turnEngine, assembler, toolRegistry, eventPublisher);

        assertNotNull(cache);
        assertNotNull(registry);
        assertNotNull(assembler);
        assertNotNull(eventPublisher);
        assertNotNull(toolRegistry);
        assertNotNull(sessionManager);
        assertNotNull(taskManager);
        assertNotNull(providerRegistry);
        assertNotNull(turnEngine);
        assertNotNull(orchestrator);
    }

    @Test
    void toolRegistryHasBuiltInTools() {
        InMemoryEventPublisher eventPublisher = config.eventPublisher();
        DefaultToolRegistry toolRegistry = config.toolRegistry(eventPublisher);
        var tools = toolRegistry.resolveTools(new ToolContext("test", "normal", Set.of()));
        assertFalse(tools.isEmpty(), "Built-in tools should be registered");

        var toolNames = tools.stream().map(t -> t.name()).toList();
        assertTrue(toolNames.contains("ask_user"), "Should have ask_user tool");
        assertTrue(toolNames.contains("file_read"), "Should have file_read tool");
        assertTrue(toolNames.contains("file_edit"), "Should have file_edit tool");
        assertTrue(toolNames.contains("shell"), "Should have shell tool");
    }

    @Test
    void modelProviderRegistryHasStubProvider() {
        DefaultModelProviderRegistry registry = config.modelProviderRegistry("stub", null);
        var providers = registry.availableProviders();
        assertTrue(providers.contains("stub"), "Should have stub provider");
        assertNotNull(registry.getProvider(null), "Default provider should be available");
        assertEquals("stub", registry.getDefaultProviderId());
    }

    @Test
    void modelProviderRegistryRegistersAiProviders() {
        ChatModel mockChatModel = mock(ChatModel.class);
        SpringAiModelProvider openai = new SpringAiModelProvider(mockChatModel, "openai");
        SpringAiModelProvider anthropic = new SpringAiModelProvider(mockChatModel, "anthropic");

        DefaultModelProviderRegistry registry = config.modelProviderRegistry(
                "openai", List.of(openai, anthropic));

        assertTrue(registry.availableProviders().contains("stub"));
        assertTrue(registry.availableProviders().contains("openai"));
        assertTrue(registry.availableProviders().contains("anthropic"));
        assertEquals("openai", registry.getDefaultProviderId());
    }

    @Test
    void modelProviderRegistryFallsBackToStubIfDefaultNotAvailable() {
        DefaultModelProviderRegistry registry = config.modelProviderRegistry("nonexistent", null);
        assertEquals("stub", registry.getDefaultProviderId(),
                "Should fall back to stub when configured default is not available");
    }
}
