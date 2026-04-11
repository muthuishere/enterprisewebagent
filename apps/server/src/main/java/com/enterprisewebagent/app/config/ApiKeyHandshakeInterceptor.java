package com.enterprisewebagent.app.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class ApiKeyHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthProperties authProperties;

    public ApiKeyHandshakeInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!authProperties.enabled()) {
            return true;
        }

        String apiKey = extractApiKeyFromQuery(request.getURI());
        if (apiKey == null) {
            apiKey = extractApiKeyFromProtocolHeader(request);
        }

        if (apiKey != null && authProperties.isValidKey(apiKey)) {
            return true;
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractApiKeyFromQuery(URI uri) {
        String query = uri.getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "apiKey".equals(kv[0]) && !kv[1].isBlank()) {
                return kv[1].trim();
            }
        }
        return null;
    }

    private String extractApiKeyFromProtocolHeader(ServerHttpRequest request) {
        List<String> protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            return protocols.getFirst().trim();
        }
        return null;
    }
}
