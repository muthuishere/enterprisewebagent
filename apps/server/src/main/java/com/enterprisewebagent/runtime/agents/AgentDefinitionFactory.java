package com.enterprisewebagent.runtime.agents;

import java.util.Set;

public class AgentDefinitionFactory {

    public static AgentDefinition generalWorker(String id, String promptContent) {
        return new AgentDefinition(id, AgentRole.GENERAL_WORKER, promptContent,
            Set.of("file_read", "file_edit", "shell", "ask_user"));
    }

    public static AgentDefinition exploreWorker(String id, String promptContent) {
        return new AgentDefinition(id, AgentRole.EXPLORE_WORKER, promptContent,
            Set.of("file_read", "shell"));
    }

    public static AgentDefinition planWorker(String id, String promptContent) {
        return new AgentDefinition(id, AgentRole.PLAN_WORKER, promptContent,
            Set.of("file_read", "shell", "ask_user"));
    }

    public static AgentDefinition verifyWorker(String id, String promptContent) {
        return new AgentDefinition(id, AgentRole.VERIFY_WORKER, promptContent,
            Set.of("file_read", "shell"));
    }

    public static AgentDefinition coordinator(String id, String promptContent) {
        return new AgentDefinition(id, AgentRole.COORDINATOR, promptContent,
            Set.of("file_read", "file_edit", "shell", "ask_user", "worker_delegate", "task_stop"));
    }
}
