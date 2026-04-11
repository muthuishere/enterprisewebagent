package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.agents.DefaultWorkerOrchestrator;
import com.enterprisewebagent.runtime.agents.WorkerOrchestrator;
import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import com.enterprisewebagent.runtime.prompt.DefaultPromptAssembler;
import com.enterprisewebagent.runtime.prompt.InMemoryPromptSectionCache;
import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptSectionCache;
import com.enterprisewebagent.runtime.prompt.PromptSectionRegistry;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.ModelProvider;
import com.enterprisewebagent.runtime.provider.StubModelProvider;
import com.enterprisewebagent.runtime.query.DefaultTurnEngine;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.session.InMemorySessionManager;
import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.tasks.InMemoryTaskManager;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;
import com.enterprisewebagent.runtime.tools.ToolRegistry;
import com.enterprisewebagent.runtime.tools.builtin.BuiltInToolRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuntimeConfig {

    @Bean
    public PromptSectionCache promptSectionCache() {
        return new InMemoryPromptSectionCache();
    }

    @Bean
    public PromptSectionRegistry promptSectionRegistry() {
        return new PromptSectionRegistry();
    }

    @Bean
    public PromptAssembler promptAssembler(PromptSectionRegistry registry, PromptSectionCache cache) {
        return new DefaultPromptAssembler(registry, cache);
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return new InMemoryEventPublisher();
    }

    @Bean
    public DefaultToolRegistry toolRegistry() {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        BuiltInToolRegistrar.registerAll(registry);
        return registry;
    }

    @Bean
    public SessionManager sessionManager() {
        return new InMemorySessionManager();
    }

    @Bean
    public TaskManager taskManager() {
        return new InMemoryTaskManager();
    }

    @Bean
    public DefaultModelProviderRegistry modelProviderRegistry() {
        DefaultModelProviderRegistry registry = new DefaultModelProviderRegistry();
        registry.register("stub", new StubModelProvider("I'm the enterprise web agent. How can I help?"));
        return registry;
    }

    @Bean
    public TurnEngine turnEngine(DefaultModelProviderRegistry providerRegistry,
                                  DefaultToolRegistry toolRegistry,
                                  InMemoryEventPublisher eventPublisher) {
        ModelProvider provider = providerRegistry.getProvider(null);
        return new DefaultTurnEngine(provider, toolRegistry, eventPublisher);
    }

    @Bean
    public WorkerOrchestrator workerOrchestrator(TurnEngine turnEngine,
                                                  PromptAssembler promptAssembler,
                                                  ToolRegistry toolRegistry,
                                                  InMemoryEventPublisher eventPublisher) {
        return new DefaultWorkerOrchestrator(turnEngine, promptAssembler, toolRegistry, eventPublisher);
    }
}
