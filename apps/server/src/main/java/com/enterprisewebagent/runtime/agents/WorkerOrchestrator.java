package com.enterprisewebagent.runtime.agents;

public interface WorkerOrchestrator {
    WorkerResult delegate(AgentDefinition worker, String task, AgentContext parentContext);
}
