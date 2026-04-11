package com.enterprisewebagent.runtime.commands;

import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;

import java.util.List;
import java.util.Map;

public record CommandContext(
        String sessionId,
        String rawInput,
        List<String> args,
        SessionManager sessionManager,
        TaskManager taskManager,
        DefaultModelProviderRegistry modelProviderRegistry,
        Map<String, Object> extra
) {
    public CommandContext(String sessionId, String rawInput, List<String> args,
                         SessionManager sessionManager, TaskManager taskManager,
                         DefaultModelProviderRegistry modelProviderRegistry) {
        this(sessionId, rawInput, args, sessionManager, taskManager, modelProviderRegistry, Map.of());
    }
}
