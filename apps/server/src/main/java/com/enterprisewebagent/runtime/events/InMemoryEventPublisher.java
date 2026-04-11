package com.enterprisewebagent.runtime.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEventPublisher implements RuntimeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    private final CopyOnWriteArrayList<RuntimeEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(RuntimeEvent event) {
        log.debug("Event published type={} listeners={}", event.getClass().getSimpleName(), listeners.size());
        for (RuntimeEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    public void addListener(RuntimeEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RuntimeEventListener listener) {
        listeners.remove(listener);
    }

    public List<RuntimeEventListener> getListeners() {
        return List.copyOf(listeners);
    }
}
