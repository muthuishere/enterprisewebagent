package com.enterprisewebagent.runtime.provider;

import java.util.List;
import java.util.Map;

public final class ProviderModels {

    public record ModelInfo(String id, String displayName, String provider) {}

    public static final Map<String, List<ModelInfo>> MODELS = Map.of(
        "openai", List.of(
            new ModelInfo("gpt-4.1", "GPT-4.1", "openai"),
            new ModelInfo("gpt-4o", "GPT-4o", "openai"),
            new ModelInfo("gpt-4o-mini", "GPT-4o Mini", "openai"),
            new ModelInfo("o3-mini", "o3-mini", "openai")
        ),
        "anthropic", List.of(
            new ModelInfo("claude-sonnet-4-5-20250929", "Claude Sonnet 4.5", "anthropic"),
            new ModelInfo("claude-opus-4-20250514", "Claude Opus 4", "anthropic"),
            new ModelInfo("claude-haiku-3-5-20241022", "Claude Haiku 3.5", "anthropic")
        ),
        "ollama", List.of(
            new ModelInfo("ollama:llama3.2", "Llama 3.2", "ollama"),
            new ModelInfo("ollama:qwen2.5", "Qwen 2.5", "ollama"),
            new ModelInfo("ollama:mistral", "Mistral", "ollama"),
            new ModelInfo("ollama:codellama", "Code Llama", "ollama")
        ),
        "copilot", List.of(
            new ModelInfo("copilot:gpt-4.1", "Copilot GPT-4.1", "copilot"),
            new ModelInfo("copilot:gpt-5-mini", "Copilot GPT-5 Mini", "copilot"),
            new ModelInfo("copilot:claude-sonnet-4", "Copilot Claude Sonnet 4", "copilot"),
            new ModelInfo("copilot:claude-opus-4.5", "Copilot Claude Opus 4.5", "copilot"),
            new ModelInfo("copilot:gemini-2.5-pro", "Copilot Gemini 2.5 Pro", "copilot")
        ),
        "codex", List.of(
            new ModelInfo("codex:gpt-5.4", "Codex GPT-5.4", "codex"),
            new ModelInfo("codex:gpt-5.4-mini", "Codex GPT-5.4 Mini", "codex"),
            new ModelInfo("codex:gpt-5.3-codex", "Codex GPT-5.3", "codex"),
            new ModelInfo("codex:gpt-5.2-codex", "Codex GPT-5.2", "codex")
        )
    );

    /** Resolve provider from model string prefix. */
    public static String resolveProvider(String model) {
        if (model == null) return null;
        if (model.startsWith("copilot:")) return "copilot";
        if (model.startsWith("codex:")) return "codex";
        if (model.startsWith("ollama:")) return "ollama";
        if (model.startsWith("claude")) return "anthropic";
        return "openai";
    }

    /** Strip prefix to get the actual model name for the provider API. */
    public static String resolveModelName(String model) {
        if (model == null) return null;
        if (model.startsWith("copilot:")) return model.substring("copilot:".length());
        if (model.startsWith("codex:")) return model.substring("codex:".length());
        if (model.startsWith("ollama:")) return model.substring("ollama:".length());
        return model;
    }

    /** Get all models as a flat list. */
    public static List<ModelInfo> allModels() {
        return MODELS.values().stream().flatMap(List::stream).toList();
    }

    private ProviderModels() {}
}
