package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final DefaultModelProviderRegistry providerRegistry;
    private final String openaiModel;
    private final String anthropicModel;

    public ConfigController(DefaultModelProviderRegistry providerRegistry,
                            @Value("${spring.ai.openai.chat.options.model:}") String openaiModel,
                            @Value("${spring.ai.anthropic.chat.options.model:}") String anthropicModel) {
        this.providerRegistry = providerRegistry;
        this.openaiModel = openaiModel;
        this.anthropicModel = anthropicModel;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("providers", providerRegistry.availableProviders());
        config.put("defaultProvider", providerRegistry.getDefaultProviderId());
        config.put("models", buildModelsMap());
        config.put("version", "0.1.0");
        config.put("runtime", "enterprisewebagent");
        return ResponseEntity.ok(config);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private Map<String, String> buildModelsMap() {
        Map<String, String> models = new LinkedHashMap<>();
        if (providerRegistry.availableProviders().contains("openai") && !openaiModel.isBlank()) {
            models.put("openai", openaiModel);
        }
        if (providerRegistry.availableProviders().contains("anthropic") && !anthropicModel.isBlank()) {
            models.put("anthropic", anthropicModel);
        }
        return models;
    }
}
