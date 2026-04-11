package com.enterprisewebagent.runtime.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEventPublisher implements RuntimeEventPublisher {

    private final InMemoryEventPublisher delegate;
    private final ExecutorService executor;

    public AsyncEventPublisher(InMemoryEventPublisher delegate) {
        this.delegate = delegate;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void publish(RuntimeEvent event) {
        executor.submit(() -> delegate.publish(event));
    }

    public void shutdown() {
        executor.shutdown();
    }
}
