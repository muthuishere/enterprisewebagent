package com.enterprisewebagent.runtime.agents.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CustomAgentLoader {

    private static final Logger log = LoggerFactory.getLogger(CustomAgentLoader.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CustomAgentDefinition> agents = new ConcurrentHashMap<>();
    private final List<Path> searchPaths;

    public CustomAgentLoader(List<Path> searchPaths) {
        this.searchPaths = List.copyOf(searchPaths);
    }

    /**
     * Default search paths: project .agents/ and ~/.enterprisewebagent/agents/
     */
    public static CustomAgentLoader withDefaults() {
        List<Path> paths = new ArrayList<>();
        paths.add(Path.of(".agents"));
        Path globalDir = Path.of(System.getProperty("user.home"), ".enterprisewebagent", "agents");
        paths.add(globalDir);
        return new CustomAgentLoader(paths);
    }

    public void loadAll() {
        agents.clear();
        for (Path dir : searchPaths) {
            if (Files.isDirectory(dir)) {
                loadFromDirectory(dir);
            }
        }
        log.info("Loaded {} custom agent definitions from {} search paths", agents.size(), searchPaths.size());
    }

    void loadFromDirectory(Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : stream) {
                try {
                    CustomAgentDefinition def = parseFile(file);
                    agents.put(def.name(), def);
                    log.debug("Loaded custom agent: {} from {}", def.name(), file);
                } catch (Exception e) {
                    log.warn("Failed to load agent definition from {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read agent directory {}: {}", directory, e.getMessage());
        }
    }

    CustomAgentDefinition parseFile(Path file) throws IOException {
        JsonNode root = objectMapper.readTree(file.toFile());

        String name = requireField(root, "name", file);
        String description = optionalField(root, "description", "");
        String systemPrompt = optionalField(root, "systemPrompt", "");
        String model = optionalField(root, "model", "copilot:gpt-4.1");

        List<String> allowedTools = new ArrayList<>();
        if (root.has("allowedTools") && root.get("allowedTools").isArray()) {
            for (JsonNode tool : root.get("allowedTools")) {
                allowedTools.add(tool.asText());
            }
        }

        Map<String, String> config = new ConcurrentHashMap<>();
        if (root.has("config") && root.get("config").isObject()) {
            root.get("config").fields().forEachRemaining(entry ->
                config.put(entry.getKey(), entry.getValue().asText())
            );
        }

        return new CustomAgentDefinition(name, description, systemPrompt, model, List.copyOf(allowedTools), Map.copyOf(config));
    }

    private String requireField(JsonNode root, String field, Path file) {
        if (!root.has(field) || root.get(field).asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field '" + field + "' in " + file);
        }
        return root.get(field).asText();
    }

    private String optionalField(JsonNode root, String field, String defaultValue) {
        return root.has(field) ? root.get(field).asText() : defaultValue;
    }

    public Optional<CustomAgentDefinition> get(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    public List<CustomAgentDefinition> listAll() {
        return List.copyOf(agents.values());
    }

    public void register(CustomAgentDefinition definition) {
        agents.put(definition.name(), definition);
    }
}
