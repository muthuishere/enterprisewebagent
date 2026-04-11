package com.enterprisewebagent.runtime.events;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EventBuffer implements RuntimeEventListener {

    private final int maxCapacity;
    private final ConcurrentLinkedDeque<RuntimeEvent> events = new ConcurrentLinkedDeque<>();

    public EventBuffer() {
        this(1000);
    }

    public EventBuffer(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        events.addLast(event);
        while (events.size() > maxCapacity) {
            events.pollFirst();
        }
    }

    public List<RuntimeEvent> getEvents() {
        return List.copyOf(events);
    }

    public List<RuntimeEvent> getEvents(String sessionId) {
        return events.stream()
                .filter(e -> sessionId.equals(extractSessionId(e)))
                .toList();
    }

    public void clear() {
        events.clear();
    }

    private static String extractSessionId(RuntimeEvent event) {
        return switch (event) {
            case TurnStartedEvent e -> e.sessionId();
            case TokenDeltaEvent e -> e.sessionId();
            case ToolRequestedEvent e -> e.sessionId();
            case ToolCompletedEvent e -> e.sessionId();
            case TaskStateChangedEvent e -> e.sessionId();
            case WorkerStateChangedEvent e -> e.sessionId();
            case TurnCompletedEvent e -> e.sessionId();
            case TurnFailedEvent e -> e.sessionId();
        };
    }
}
