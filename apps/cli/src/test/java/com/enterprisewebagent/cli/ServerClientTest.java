package com.enterprisewebagent.cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerClientTest {

    @Test
    void testBaseUrlConstruction() {
        var client = new ServerClient("http://localhost:8080");
        assertEquals("http://localhost:8080", client.getBaseUrl());
    }

    @Test
    void testBaseUrlTrailingSlashStripped() {
        var client = new ServerClient("http://localhost:8080/");
        assertEquals("http://localhost:8080", client.getBaseUrl());
    }

    @Test
    void testGetThrowsOnConnectionRefused() {
        var client = new ServerClient("http://localhost:19999");
        var ex = assertThrows(Exception.class, () -> client.get("/api/sessions"));
        assertTrue(ex.getMessage().contains("Server not reachable") || ex.getMessage().contains("Connection refused"),
            "Expected connection error, got: " + ex.getMessage());
    }

    @Test
    void testPostThrowsOnConnectionRefused() {
        var client = new ServerClient("http://localhost:19999");
        var ex = assertThrows(Exception.class, () ->
            client.post("/api/sessions", java.util.Map.of("key", "value")));
        assertTrue(ex.getMessage().contains("Server not reachable") || ex.getMessage().contains("Connection refused"),
            "Expected connection error, got: " + ex.getMessage());
    }

    @Test
    void testPutThrowsOnConnectionRefused() {
        var client = new ServerClient("http://localhost:19999");
        var ex = assertThrows(Exception.class, () ->
            client.put("/api/tasks/1/status", java.util.Map.of("status", "RUNNING")));
        assertTrue(ex.getMessage().contains("Server not reachable") || ex.getMessage().contains("Connection refused"),
            "Expected connection error, got: " + ex.getMessage());
    }

    @Test
    void testGsonIsAvailable() {
        var client = new ServerClient("http://localhost:8080");
        assertNotNull(client.gson());
    }
}
