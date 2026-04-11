package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class WebFetchTool implements ToolExecutor {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int DEFAULT_MAX_LENGTH = 5000;

    @Override
    public String toolName() {
        return "web_fetch";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object urlObj = args.get("url");
        if (urlObj == null || urlObj.toString().isBlank()) {
            return new ToolResult(toolName(), "Missing required parameter: url", false);
        }

        String url = urlObj.toString();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return new ToolResult(toolName(), "Invalid URL: must start with http:// or https://", false);
        }

        int maxLength = DEFAULT_MAX_LENGTH;
        if (args.containsKey("max_length")) {
            try {
                maxLength = Integer.parseInt(args.get("max_length").toString());
                if (maxLength < 1) maxLength = 1;
            } catch (NumberFormatException e) {
                // use default
            }
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "enterprisewebagent/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                return new ToolResult(toolName(), "HTTP error: " + response.statusCode(), false);
            }

            String body = response.body();
            String plainText = stripHtmlTags(body);

            if (plainText.length() > maxLength) {
                plainText = plainText.substring(0, maxLength) + "\n... [truncated]";
            }

            return new ToolResult(toolName(), plainText, true);
        } catch (IllegalArgumentException e) {
            return new ToolResult(toolName(), "Invalid URL: " + e.getMessage(), false);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Fetch failed: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolResult(toolName(), "Fetch interrupted", false);
        }
    }

    private String stripHtmlTags(String html) {
        // Remove script and style blocks
        String cleaned = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        cleaned = cleaned.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        // Remove HTML tags
        cleaned = cleaned.replaceAll("<[^>]+>", " ");
        // Decode common entities
        cleaned = cleaned.replaceAll("&amp;", "&").replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">").replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'").replaceAll("&nbsp;", " ");
        // Collapse whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
}
