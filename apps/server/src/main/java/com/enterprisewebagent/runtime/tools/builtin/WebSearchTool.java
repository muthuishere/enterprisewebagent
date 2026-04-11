package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSearchTool implements ToolExecutor {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__a\"[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern SNIPPET_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL);

    @Override
    public String toolName() {
        return "web_search";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();

        Object queryObj = args.get("query");
        if (queryObj == null || queryObj.toString().isBlank()) {
            return new ToolResult(toolName(), "Missing required parameter: query", false);
        }

        String query = queryObj.toString();
        int maxResults = DEFAULT_MAX_RESULTS;
        if (args.containsKey("max_results")) {
            try {
                maxResults = Integer.parseInt(args.get("max_results").toString());
                if (maxResults < 1) maxResults = 1;
                if (maxResults > 20) maxResults = 20;
            } catch (NumberFormatException e) {
                // use default
            }
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

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

            if (response.statusCode() != 200) {
                return new ToolResult(toolName(), "Search request failed with status: " + response.statusCode(), false);
            }

            String body = response.body();
            List<String> results = parseResults(body, maxResults);

            if (results.isEmpty()) {
                return new ToolResult(toolName(), "No results found for: " + query, true);
            }

            return new ToolResult(toolName(), String.join("\n\n", results), true);
        } catch (IOException e) {
            return new ToolResult(toolName(), "Search request failed: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolResult(toolName(), "Search request interrupted", false);
        }
    }

    private List<String> parseResults(String html, int maxResults) {
        List<String> results = new ArrayList<>();
        Matcher linkMatcher = RESULT_PATTERN.matcher(html);
        Matcher snippetMatcher = SNIPPET_PATTERN.matcher(html);

        while (linkMatcher.find() && results.size() < maxResults) {
            String href = linkMatcher.group(1);
            String title = stripHtmlTags(linkMatcher.group(2)).trim();
            String snippet = "";
            if (snippetMatcher.find()) {
                snippet = stripHtmlTags(snippetMatcher.group(1)).trim();
            }
            results.add(title + "\n" + href + "\n" + snippet);
        }

        return results;
    }

    private String stripHtmlTags(String html) {
        return html.replaceAll("<[^>]+>", "").replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"").replaceAll("&#39;", "'");
    }
}
