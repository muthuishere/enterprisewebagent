package com.enterprisewebagent.runtime.prompt;

public enum PromptCatalog {
    SYSTEM_IDENTITY("prompt_001_system_identity", true),
    SYSTEM_RULES("prompt_002_system_rules", true),
    DOING_TASKS("prompt_003_doing_tasks", true),
    ACTION_SAFETY("prompt_004_action_safety", true),
    TOOL_USAGE("prompt_005_tool_usage", true),
    SESSION_GUIDANCE("prompt_006_session_guidance", false),
    COORDINATOR_MODE("prompt_007_coordinator_mode", true),
    WORKER_GENERAL("prompt_008_worker_general", true),
    WORKER_EXPLORE("prompt_009_worker_explore", true),
    WORKER_PLAN("prompt_010_worker_plan", true),
    WORKER_VERIFY("prompt_011_worker_verify", true),
    ASK_USER("prompt_012_ask_user", false);

    private final String catalogKey;
    private final boolean cached;

    PromptCatalog(String catalogKey, boolean cached) {
        this.catalogKey = catalogKey;
        this.cached = cached;
    }

    public String catalogKey() {
        return catalogKey;
    }

    public boolean cached() {
        return cached;
    }

    public PromptSection toSection(String content) {
        return new PromptSection(catalogKey, content, cached);
    }
}
