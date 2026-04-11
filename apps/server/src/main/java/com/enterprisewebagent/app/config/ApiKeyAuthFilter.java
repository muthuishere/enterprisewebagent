package com.enterprisewebagent.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/config/health",
            "/actuator/health"
    );
    private static final String AUTH_HEADER = "Authorization";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthProperties authProperties;

    public ApiKeyAuthFilter(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!authProperties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (PUBLIC_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = extractApiKey(request);
        if (apiKey != null && authProperties.isValidKey(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
    }

    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return apiKeyHeader.trim();
        }

        return null;
    }
}
