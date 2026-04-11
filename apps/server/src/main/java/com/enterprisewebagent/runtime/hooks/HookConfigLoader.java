package com.enterprisewebagent.runtime.hooks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HookConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(HookConfigLoader.class);
    private static final String PROJECT_HOOKS_PATH = ".agent/hooks.json";
    private static final String GLOBAL_HOOKS_PATH = ".enterprisewebagent/hooks.json";

    private final ObjectMapper objectMapper;

    public HookConfigLoader() {
        this.objectMapper = new ObjectMapper();
    }

    public HookConfigLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Hook> loadHooks(Path projectRoot) {
        List<Hook> hooks = new ArrayList<>();

        // Load global hooks
        Path globalConfig = Path.of(System.getProperty("user.home")).resolve(GLOBAL_HOOKS_PATH);
        hooks.addAll(loadFromFile(globalConfig, "global"));

        // Load project hooks (override global)
        if (projectRoot != null) {
            Path projectConfig = projectRoot.resolve(PROJECT_HOOKS_PATH);
            hooks.addAll(loadFromFile(projectConfig, "project"));
        }

        return hooks;
    }

    public List<Hook> loadFromFile(Path configPath, String source) {
        if (!Files.exists(configPath) || !Files.isRegularFile(configPath)) {
            return List.of();
        }

        try {
            String json = Files.readString(configPath);
            List<Map<String, Object>> entries = objectMapper.readValue(json, new TypeReference<>() {});

            List<Hook> hooks = new ArrayList<>();
            for (Map<String, Object> entry : entries) {
                Hook hook = parseHookEntry(entry, source);
                if (hook != null) {
                    hooks.add(hook);
                }
            }

            log.info("Loaded {} hooks from {}", hooks.size(), configPath);
            return hooks;
        } catch (IOException e) {
            log.error("Failed to load hooks from {}: {}", configPath, e.getMessage());
            return List.of();
        }
    }

    private Hook parseHookEntry(Map<String, Object> entry, String source) {
        try {
            String typeStr = (String) entry.get("type");
            String command = (String) entry.get("command");

            if (typeStr == null || command == null) {
                log.warn("Hook entry missing type or command, skipping");
                return null;
            }

            HookType type = HookType.valueOf(typeStr.toUpperCase());
            String id = entry.containsKey("id")
                    ? (String) entry.get("id")
                    : source + "_" + UUID.randomUUID().toString().substring(0, 8);
            int order = entry.containsKey("order")
                    ? ((Number) entry.get("order")).intValue()
                    : 0;
            boolean enabled = !entry.containsKey("enabled") || Boolean.parseBoolean(entry.get("enabled").toString());

            return new Hook(id, type, command, order, enabled);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid hook type in entry: {}", entry.get("type"));
            return null;
        }
    }
}
