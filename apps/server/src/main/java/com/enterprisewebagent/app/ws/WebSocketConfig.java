package com.enterprisewebagent.app.ws;

import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final InMemoryEventPublisher eventPublisher;

    public WebSocketConfig(InMemoryEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new StreamingWebSocketHandler(eventPublisher), "/ws/stream")
                .setAllowedOrigins("*");
    }
}
