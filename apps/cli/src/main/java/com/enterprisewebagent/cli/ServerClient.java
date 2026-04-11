package com.enterprisewebagent.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class ServerClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    public ServerClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String get(String path) throws IOException, InterruptedException {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return response.body();
        } catch (ConnectException e) {
            throw new IOException("Server not reachable at " + baseUrl, e);
        }
    }

    public String post(String path, Map<String, String> body) throws IOException, InterruptedException {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .header("Content-Type", "application/json")
                .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return response.body();
        } catch (ConnectException e) {
            throw new IOException("Server not reachable at " + baseUrl, e);
        }
    }

    public String put(String path, Map<String, String> body) throws IOException, InterruptedException {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .header("Content-Type", "application/json")
                .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return response.body();
        } catch (ConnectException e) {
            throw new IOException("Server not reachable at " + baseUrl, e);
        }
    }

    public Map<String, Object> getJson(String path) throws IOException, InterruptedException {
        String raw = get(path);
        return gson.fromJson(raw, MAP_TYPE);
    }

    public Map<String, Object> postJson(String path, Map<String, String> body) throws IOException, InterruptedException {
        String raw = post(path, body);
        return gson.fromJson(raw, MAP_TYPE);
    }

    public Map<String, Object> putJson(String path, Map<String, String> body) throws IOException, InterruptedException {
        String raw = put(path, body);
        return gson.fromJson(raw, MAP_TYPE);
    }

    public Gson gson() { return gson; }

    private void checkStatus(HttpResponse<String> response) throws IOException {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Server returned HTTP " + status + ": " + response.body());
        }
    }
}
