package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.agents.WorkerOrchestrator;
import com.enterprisewebagent.runtime.agents.custom.CustomAgentLoader;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.teams.TeamRegistry;
import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;

public class AgentTeamToolRegistrar {

    public static void registerAll(
            DefaultToolRegistry registry,
            WorkerOrchestrator orchestrator,
            TaskManager taskManager,
            TeamRegistry teamRegistry
    ) {
        CustomAgentLoader agentLoader = CustomAgentLoader.withDefaults();
        agentLoader.loadAll();

        registry.registerExecutor(new AgentTool(orchestrator, taskManager, agentLoader));
        registry.registerExecutor(new TeamCreateTool(teamRegistry));
        registry.registerExecutor(new TeamDeleteTool(teamRegistry));
        registry.registerExecutor(new SendMessageTool(teamRegistry));
    }

    public static void registerAll(
            DefaultToolRegistry registry,
            WorkerOrchestrator orchestrator,
            TaskManager taskManager,
            TeamRegistry teamRegistry,
            CustomAgentLoader agentLoader
    ) {
        registry.registerExecutor(new AgentTool(orchestrator, taskManager, agentLoader));
        registry.registerExecutor(new TeamCreateTool(teamRegistry));
        registry.registerExecutor(new TeamDeleteTool(teamRegistry));
        registry.registerExecutor(new SendMessageTool(teamRegistry));
    }
}
