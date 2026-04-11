package com.enterprisewebagent.runtime.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEventPublisher implements RuntimeEventPublisher {

    private final CopyOnWriteArrayList<RuntimeEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(RuntimeEvent event) {
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
