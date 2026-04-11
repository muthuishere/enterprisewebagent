package com.enterprisewebagent.app.ws;

import com.enterprisewebagent.runtime.events.EventSerializer;
import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import com.enterprisewebagent.runtime.events.RuntimeEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StreamingWebSocketHandler extends TextWebSocketHandler {

    private final InMemoryEventPublisher eventPublisher;
    private final Map<String, RuntimeEventListener> listenersByWsSession = new ConcurrentHashMap<>();

    public StreamingWebSocketHandler(InMemoryEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        RuntimeEventListener listener = event -> {
            String json = EventSerializer.toJson(event);
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                // Connection may have closed; ignore
            }
        };

        listenersByWsSession.put(session.getId(), listener);
        eventPublisher.addListener(listener);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Client messages are not processed in this version
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        RuntimeEventListener listener = listenersByWsSession.remove(session.getId());
        if (listener != null) {
            eventPublisher.removeListener(listener);
        }
    }

    static String extractSessionId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return "unknown";
        String query = uri.getQuery();
        if (query != null && query.contains("sessionId=")) {
            return query.split("sessionId=")[1].split("&")[0];
        }
        return "unknown";
    }
}
