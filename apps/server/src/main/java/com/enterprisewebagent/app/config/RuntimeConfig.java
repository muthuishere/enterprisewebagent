package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.agents.DefaultWorkerOrchestrator;
import com.enterprisewebagent.runtime.agents.WorkerOrchestrator;
import com.enterprisewebagent.runtime.commands.CommandDispatcher;
import com.enterprisewebagent.runtime.commands.CommandRegistry;
import com.enterprisewebagent.runtime.commands.builtin.BuiltInCommandRegistrar;
import com.enterprisewebagent.runtime.cost.CostCalculator;
import com.enterprisewebagent.runtime.cost.SessionCostTracker;
import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import com.enterprisewebagent.runtime.permissions.*;
import com.enterprisewebagent.runtime.planning.InMemoryPlanManager;
import com.enterprisewebagent.runtime.planning.PlanManager;
import com.enterprisewebagent.runtime.planning.PlanModeToolFilter;
import com.enterprisewebagent.runtime.prompt.DefaultPromptAssembler;
import com.enterprisewebagent.runtime.prompt.InMemoryPromptSectionCache;
import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptSectionCache;
import com.enterprisewebagent.runtime.prompt.PromptSectionRegistry;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.ModelProvider;
import com.enterprisewebagent.runtime.provider.SpringAiModelProvider;
import com.enterprisewebagent.runtime.provider.StubModelProvider;
import com.enterprisewebagent.runtime.query.DefaultTurnEngine;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;
import com.enterprisewebagent.runtime.tools.ToolRegistry;
import com.enterprisewebagent.runtime.tools.builtin.BuiltInToolRegistrar;
import com.enterprisewebagent.runtime.tools.builtin.PlanningToolRegistrar;
import com.enterprisewebagent.app.persistence.JpaSessionManager;
import com.enterprisewebagent.app.persistence.JpaTaskManager;
import com.enterprisewebagent.app.persistence.SessionRepository;
import com.enterprisewebagent.app.persistence.TaskRepository;
import com.enterprisewebagent.app.persistence.TranscriptEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

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
    public PlanManager planManager() {
        return new InMemoryPlanManager();
    }

    @Bean
    public DefaultToolRegistry toolRegistry(InMemoryEventPublisher eventPublisher, PlanManager planManager) {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        BuiltInToolRegistrar.registerAll(registry, eventPublisher);
        PlanningToolRegistrar.registerAll(registry, planManager);
        registry.addFilter(new PlanModeToolFilter(planManager));
        return registry;
    }

    @Bean
    public SessionManager sessionManager(SessionRepository sessionRepository,
                                          TranscriptEntryRepository transcriptEntryRepository) {
        return new JpaSessionManager(sessionRepository, transcriptEntryRepository);
    }

    @Bean
    public TaskManager taskManager(TaskRepository taskRepository) {
        return new JpaTaskManager(taskRepository);
    }

    @Bean
    public DefaultModelProviderRegistry modelProviderRegistry(
            @Value("${app.runtime.default-provider:stub}") String defaultProvider,
            @Autowired(required = false) List<SpringAiModelProvider> aiProviders) {
        DefaultModelProviderRegistry registry = new DefaultModelProviderRegistry();
        registry.register("stub", new StubModelProvider("I'm the enterprise web agent. How can I help?"));
        if (aiProviders != null) {
            for (SpringAiModelProvider provider : aiProviders) {
                registry.register(provider.providerId(), provider);
            }
        }
        if (registry.availableProviders().contains(defaultProvider)) {
            registry.setDefault(defaultProvider);
        }
        return registry;
    }

    @Bean
    public AtomicReference<PermissionMode> permissionMode() {
        return new AtomicReference<>(PermissionMode.AUTO_APPROVE);
    }

    @Bean
    public CopyOnWriteArrayList<PermissionRule> permissionRules() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean
    public BashSafetyAnalyzer bashSafetyAnalyzer() {
        return new BashSafetyAnalyzer();
    }

    @Bean
    public FilePathChecker filePathChecker(CopyOnWriteArrayList<PermissionRule> permissionRules) {
        return new FilePathChecker(permissionRules);
    }

    @Bean
    public DenialTracker denialTracker() {
        return new DenialTracker();
    }

    @Bean
    public PermissionEvaluator permissionEvaluator(AtomicReference<PermissionMode> permissionMode,
                                                    BashSafetyAnalyzer bashSafetyAnalyzer,
                                                    FilePathChecker filePathChecker,
                                                    CopyOnWriteArrayList<PermissionRule> permissionRules) {
        return new PermissionEvaluator(permissionMode.get(), bashSafetyAnalyzer, filePathChecker, permissionRules);
    }

    @Bean
    public PermissionToolFilter permissionToolFilter(PermissionEvaluator permissionEvaluator) {
        return new PermissionToolFilter(permissionEvaluator);
    }

    @Bean
    public CostCalculator costCalculator() {
        return new CostCalculator();
    }

    @Bean
    public SessionCostTracker sessionCostTracker() {
        return new SessionCostTracker();
    }

    @Bean
    public TurnEngine turnEngine(DefaultModelProviderRegistry providerRegistry,
                                  DefaultToolRegistry toolRegistry,
                                  InMemoryEventPublisher eventPublisher,
                                  RuntimeMetrics runtimeMetrics,
                                  PermissionEvaluator permissionEvaluator,
                                  DenialTracker denialTracker,
                                  CostCalculator costCalculator,
                                  SessionCostTracker sessionCostTracker) {
        TurnEngine delegate = new DefaultTurnEngine(providerRegistry, toolRegistry, eventPublisher,
                permissionEvaluator, denialTracker, costCalculator, sessionCostTracker);
        return new ObservableTurnEngine(delegate, runtimeMetrics);
    }

    @Bean
    public CommandRegistry commandRegistry() {
        CommandRegistry registry = new CommandRegistry();
        BuiltInCommandRegistrar.registerAll(registry);
        return registry;
    }

    @Bean
    public CommandDispatcher commandDispatcher(CommandRegistry commandRegistry) {
        return new CommandDispatcher(commandRegistry);
    }

    @Bean
    public WorkerOrchestrator workerOrchestrator(TurnEngine turnEngine,
                                                  PromptAssembler promptAssembler,
                                                  ToolRegistry toolRegistry,
                                                  InMemoryEventPublisher eventPublisher) {
        return new DefaultWorkerOrchestrator(turnEngine, promptAssembler, toolRegistry, eventPublisher);
    }
}
