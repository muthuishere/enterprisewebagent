package com.enterprisewebagent.runtime.provider;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StubModelProvider implements ModelProvider {

    private static final String FALLBACK = "No more stub responses configured";
    private final Queue<String> responses;

    public StubModelProvider(String... responses) {
        this.responses = new ConcurrentLinkedQueue<>(List.of(responses));
    }

    @Override
    public String complete(ModelRequest request) {
        String response = responses.poll();
        return response != null ? response : FALLBACK;
    }

    @Override
    public Flux<String> stream(ModelRequest request) {
        String response = responses.poll();
        if (response == null) {
            response = FALLBACK;
        }
        String[] words = response.split("(?<=\\s)");
        return Flux.fromArray(words);
    }
}
