package com.enterprisewebagent.app.ws;

import com.enterprisewebagent.app.config.ApiKeyHandshakeInterceptor;
import com.enterprisewebagent.app.config.AuthProperties;
import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final InMemoryEventPublisher eventPublisher;
    private final AuthProperties authProperties;

    public WebSocketConfig(InMemoryEventPublisher eventPublisher, AuthProperties authProperties) {
        this.eventPublisher = eventPublisher;
        this.authProperties = authProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new StreamingWebSocketHandler(eventPublisher), "/ws/stream")
                .addInterceptors(new ApiKeyHandshakeInterceptor(authProperties))
                .setAllowedOrigins("*");
    }
}
