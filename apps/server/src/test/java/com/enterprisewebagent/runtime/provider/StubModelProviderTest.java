package com.enterprisewebagent.runtime.provider;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StubModelProviderTest {

    private ModelRequest dummyRequest() {
        return new ModelRequest(
                List.of(new PromptSection("core", "system prompt", false)),
                "test-model",
                Map.of()
        );
    }

    @Test
    void completeReturnsResponsesInOrder() {
        StubModelProvider provider = new StubModelProvider("first", "second", "third");

        assertEquals("first", provider.complete(dummyRequest()));
        assertEquals("second", provider.complete(dummyRequest()));
        assertEquals("third", provider.complete(dummyRequest()));
    }

    @Test
    void completeReturnsFallbackWhenExhausted() {
        StubModelProvider provider = new StubModelProvider("only");

        assertEquals("only", provider.complete(dummyRequest()));
        assertEquals("No more stub responses configured", provider.complete(dummyRequest()));
    }

    @Test
    void streamReturnsChunkedContent() {
        StubModelProvider provider = new StubModelProvider("hello world test");

        List<String> chunks = provider.stream(dummyRequest()).collectList().block();

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        String joined = String.join("", chunks);
        assertEquals("hello world test", joined);
    }

    @Test
    void streamReturnsFallbackWhenExhausted() {
        StubModelProvider provider = new StubModelProvider();

        List<String> chunks = provider.stream(dummyRequest()).collectList().block();

        assertNotNull(chunks);
        String joined = String.join("", chunks);
        assertEquals("No more stub responses configured", joined);
    }
}
