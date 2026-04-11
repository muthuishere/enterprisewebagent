package com.enterprisewebagent.runtime.provider;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);

    public record AvailableModel(String id, String displayName, String provider, boolean available) {}
    public record ProviderStatus(String provider, boolean available, String reason) {}

    private final DefaultModelProviderRegistry providerRegistry;
    private final String ollamaBaseUrl;

    private final CopyOnWriteArrayList<AvailableModel> availableModels = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ProviderStatus> providerStatuses = new CopyOnWriteArrayList<>();

    public ModelRegistry(DefaultModelProviderRegistry providerRegistry, String ollamaBaseUrl) {
        this.providerRegistry = providerRegistry;
        this.ollamaBaseUrl = ollamaBaseUrl != null ? ollamaBaseUrl : "http://localhost:11434";
    }

    public ModelRegistry(DefaultModelProviderRegistry providerRegistry) {
        this(providerRegistry, null);
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        List<ProviderStatus> statuses = new ArrayList<>();
        List<AvailableModel> models = new ArrayList<>();

        for (String provider : ProviderModels.MODELS.keySet()) {
            ProviderStatus status = checkProvider(provider);
            statuses.add(status);

            List<ProviderModels.ModelInfo> catalogModels = ProviderModels.MODELS.get(provider);
            for (ProviderModels.ModelInfo info : catalogModels) {
                models.add(new AvailableModel(info.id(), info.displayName(), info.provider(), status.available()));
            }
        }

        this.providerStatuses.clear();
        this.providerStatuses.addAll(statuses);
        this.availableModels.clear();
        this.availableModels.addAll(models);

        log.info("ModelRegistry refreshed: {} providers, {} models ({} available)",
                statuses.size(),
                models.size(),
                models.stream().filter(AvailableModel::available).count());
    }

    public List<AvailableModel> getAvailableModels() {
        return List.copyOf(availableModels);
    }

    public List<ProviderStatus> getProviderStatuses() {
        return List.copyOf(providerStatuses);
    }

    ProviderStatus checkProvider(String provider) {
        return switch (provider) {
            case "openai" -> checkRegistered("openai");
            case "anthropic" -> checkRegistered("anthropic");
            case "ollama" -> checkOllama();
            case "copilot" -> checkCopilot();
            case "codex" -> checkCodex();
            default -> new ProviderStatus(provider, false, "Unknown provider");
        };
    }

    private ProviderStatus checkRegistered(String providerId) {
        boolean registered = providerRegistry.availableProviders().contains(providerId);
        return new ProviderStatus(providerId, registered,
                registered ? "API key configured" : "API key not configured");
    }

    private ProviderStatus checkOllama() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() == 200;
            return new ProviderStatus("ollama", ok,
                    ok ? "Ollama running at " + ollamaBaseUrl : "Ollama returned status " + response.statusCode());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ProviderStatus("ollama", false, "Ollama not reachable: " + e.getMessage());
        }
    }

    private ProviderStatus checkCopilot() {
        try {
            Process process = new ProcessBuilder("gh", "auth", "token")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return new ProviderStatus("copilot", true, "GitHub CLI authenticated");
            }
            return new ProviderStatus("copilot", false, "gh auth token failed");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ProviderStatus("copilot", false, "gh CLI not available: " + e.getMessage());
        }
    }

    private ProviderStatus checkCodex() {
        Path authFile = Path.of(System.getProperty("user.home"), ".codex", "auth.json");
        boolean exists = Files.exists(authFile);
        return new ProviderStatus("codex", exists,
                exists ? "Codex auth file found" : "~/.codex/auth.json not found");
    }
}
