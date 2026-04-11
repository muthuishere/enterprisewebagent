package com.enterprisewebagent.runtime.agents;

import com.enterprisewebagent.runtime.events.RuntimeEventPublisher;
import com.enterprisewebagent.runtime.events.WorkerStateChangedEvent;
import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptContext;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.query.TurnRequest;
import com.enterprisewebagent.runtime.query.TurnResult;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolRegistry;

import java.util.List;

public class DefaultWorkerOrchestrator implements WorkerOrchestrator {

    private final TurnEngine turnEngine;
    private final PromptAssembler promptAssembler;
    private final ToolRegistry toolRegistry;
    private final RuntimeEventPublisher eventPublisher;

    public DefaultWorkerOrchestrator(
        TurnEngine turnEngine,
        PromptAssembler promptAssembler,
        ToolRegistry toolRegistry,
        RuntimeEventPublisher eventPublisher
    ) {
        this.turnEngine = turnEngine;
        this.promptAssembler = promptAssembler;
        this.toolRegistry = toolRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public WorkerResult delegate(AgentDefinition worker, String task, AgentContext parentContext) {
        if (!parentContext.canNest()) {
            return new WorkerResult(worker.id(), "Max nesting depth exceeded", false, List.of());
        }

        eventPublisher.publish(new WorkerStateChangedEvent(parentContext.sessionId(), worker.id(), "started"));

        try {
            PromptContext workerPromptContext = WorkerPromptResolver.buildWorkerPromptContext(
                worker, parentContext, task
            );

            List<PromptSection> assembledPrompt = promptAssembler.assemble(workerPromptContext);

            ToolContext workerToolContext = new ToolContext(
                parentContext.sessionId(), "worker", worker.allowedTools()
            );

            List<ToolDefinition> availableTools = toolRegistry.resolveTools(workerToolContext);

            TurnRequest turnRequest = new TurnRequest(
                parentContext.sessionId(), task, assembledPrompt, availableTools
            );

            TurnResult turnResult = turnEngine.executeTurn(turnRequest);

            String state = turnResult.completed() ? "completed" : "failed";
            eventPublisher.publish(new WorkerStateChangedEvent(parentContext.sessionId(), worker.id(), state));

            return new WorkerResult(
                worker.id(),
                turnResult.output(),
                turnResult.completed(),
                turnResult.toolCalls()
            );
        } catch (Exception e) {
            eventPublisher.publish(new WorkerStateChangedEvent(parentContext.sessionId(), worker.id(), "failed"));
            return new WorkerResult(worker.id(), e.getMessage(), false, List.of());
        }
    }
}
