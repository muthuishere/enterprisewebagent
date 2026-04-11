package com.enterprisewebagent.runtime.provider;

import reactor.core.publisher.Flux;

public interface ModelProvider {
    Flux<String> stream(ModelRequest request);
    String complete(ModelRequest request);
}
