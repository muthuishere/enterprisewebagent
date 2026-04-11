package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.ProviderModels;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final DefaultModelProviderRegistry providerRegistry;

    public ConfigController(DefaultModelProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("providers", providerRegistry.availableProviders());
        config.put("defaultProvider", providerRegistry.getDefaultProviderId());
        config.put("models", buildFullModelCatalog());
        config.put("version", "0.1.0");
        config.put("runtime", "enterprisewebagent");
        return ResponseEntity.ok(config);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshProviders() {
        return getConfig();
    }

    private List<Map<String, Object>> buildFullModelCatalog() {
        List<Map<String, Object>> models = new ArrayList<>();
        Set<String> available = new HashSet<>(providerRegistry.availableProviders());

        for (var entry : ProviderModels.MODELS.entrySet()) {
            String provider = entry.getKey();
            boolean providerAvailable = available.contains(provider);
            for (var modelInfo : entry.getValue()) {
                models.add(Map.of(
                        "id", modelInfo.id(),
                        "displayName", modelInfo.displayName(),
                        "provider", modelInfo.provider(),
                        "available", providerAvailable
                ));
            }
        }
        return models;
    }
}
