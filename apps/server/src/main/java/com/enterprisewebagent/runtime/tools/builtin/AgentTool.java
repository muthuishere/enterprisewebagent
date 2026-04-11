package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.agents.AgentContext;
import com.enterprisewebagent.runtime.agents.AgentDefinition;
import com.enterprisewebagent.runtime.agents.AgentRole;
import com.enterprisewebagent.runtime.agents.WorkerOrchestrator;
import com.enterprisewebagent.runtime.agents.WorkerResult;
import com.enterprisewebagent.runtime.agents.custom.CustomAgentDefinition;
import com.enterprisewebagent.runtime.agents.custom.CustomAgentLoader;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tasks.TaskStatus;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AgentTool implements ToolExecutor {

    private final WorkerOrchestrator orchestrator;
    private final TaskManager taskManager;
    private final CustomAgentLoader agentLoader;

    public AgentTool(WorkerOrchestrator orchestrator, TaskManager taskManager, CustomAgentLoader agentLoader) {
        this.orchestrator = orchestrator;
        this.taskManager = taskManager;
        this.agentLoader = agentLoader;
    }

    @Override
    public String toolName() {
        return "agent";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object nameObj = args.get("name");
        if (nameObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: name", false);
        }

        Object promptObj = args.get("prompt");
        if (promptObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: prompt", false);
        }

        String name = nameObj.toString();
        String prompt = promptObj.toString();
        String model = args.containsKey("model") ? args.get("model").toString() : null;
        boolean background = args.containsKey("background")
                && Boolean.parseBoolean(args.get("background").toString());

        Optional<CustomAgentDefinition> definitionOpt = agentLoader.get(name);

        String resolvedModel;
        String systemPrompt;
        Set<String> tools;

        if (definitionOpt.isPresent()) {
            CustomAgentDefinition def = definitionOpt.get();
            resolvedModel = model != null ? model : def.model();
            systemPrompt = def.systemPrompt();
            tools = Set.copyOf(def.allowedTools());
        } else {
            resolvedModel = model != null ? model : "copilot:gpt-4.1";
            systemPrompt = prompt;
            tools = Set.of();
        }

        AgentDefinition agentDef = new AgentDefinition(
                "custom-" + name,
                AgentRole.GENERAL_WORKER,
                systemPrompt,
                tools
        );

        AgentContext agentContext = new AgentContext(
                context.sessionId(),
                AgentRole.GENERAL_WORKER,
                Map.of("model", resolvedModel)
        );

        if (background) {
            var task = taskManager.create(context.sessionId(), "Agent: " + name + " — " + prompt);
            taskManager.updateStatus(task.id(), TaskStatus.RUNNING);

            return new ToolResult(toolName(),
                    "Background agent '" + name + "' started. Task ID: " + task.id(), true);
        }

        WorkerResult result = orchestrator.delegate(agentDef, prompt, agentContext);
        return new ToolResult(toolName(), result.output(), result.success());
    }
}
