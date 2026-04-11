package com.enterprisewebagent.runtime.agents;

import com.enterprisewebagent.runtime.prompt.PromptCatalog;
import com.enterprisewebagent.runtime.prompt.PromptContext;
import com.enterprisewebagent.runtime.prompt.PromptPrecedence;

import java.util.HashMap;
import java.util.Map;

public class WorkerPromptResolver {

    public static PromptCatalog resolvePromptCatalog(AgentRole role) {
        return switch (role) {
            case COORDINATOR -> PromptCatalog.COORDINATOR_MODE;
            case GENERAL_WORKER -> PromptCatalog.WORKER_GENERAL;
            case EXPLORE_WORKER -> PromptCatalog.WORKER_EXPLORE;
            case PLAN_WORKER -> PromptCatalog.WORKER_PLAN;
            case VERIFY_WORKER -> PromptCatalog.WORKER_VERIFY;
        };
    }

    public static PromptContext buildWorkerPromptContext(
        AgentDefinition worker,
        AgentContext parentContext,
        String taskDescription
    ) {
        PromptPrecedence precedence = (worker.role() == AgentRole.COORDINATOR)
            ? PromptPrecedence.COORDINATOR
            : PromptPrecedence.CUSTOM_AGENT;

        Map<String, String> dynamicSections = new HashMap<>(parentContext.inheritedContext());
        dynamicSections.put("worker_task", taskDescription);

        return new PromptContext(
            parentContext.sessionId(),
            precedence,
            dynamicSections,
            null,
            worker.role() == AgentRole.COORDINATOR ? worker.promptSurface() : null,
            worker.role() != AgentRole.COORDINATOR ? worker.promptSurface() : null,
            null,
            null,
            null
        );
    }
}
