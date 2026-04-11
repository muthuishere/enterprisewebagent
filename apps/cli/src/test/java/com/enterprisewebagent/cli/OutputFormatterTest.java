package com.enterprisewebagent.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OutputFormatterTest {

    @Test
    void testPrintSuccess() {
        var out = new ByteArrayOutputStream();
        var oldOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            OutputFormatter.printSuccess("it worked");
            assertTrue(out.toString().contains("✓ it worked"));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void testPrintError() {
        var err = new ByteArrayOutputStream();
        var oldErr = System.err;
        System.setErr(new PrintStream(err));
        try {
            OutputFormatter.printError("something failed");
            assertTrue(err.toString().contains("✗ something failed"));
        } finally {
            System.setErr(oldErr);
        }
    }

    @Test
    void testPrintSession() {
        var out = new ByteArrayOutputStream();
        var oldOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("id", "sess-123");
            session.put("status", "OPEN");
            session.put("workspaceId", "default");
            session.put("created", "2025-01-01T00:00:00Z");
            OutputFormatter.printSession(session);
            var output = out.toString();
            assertTrue(output.contains("sess-123"));
            assertTrue(output.contains("OPEN"));
            assertTrue(output.contains("default"));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void testPrintTask() {
        var out = new ByteArrayOutputStream();
        var oldOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", "task-456");
            task.put("status", "RUNNING");
            task.put("description", "Do something");
            OutputFormatter.printTask(task);
            var output = out.toString();
            assertTrue(output.contains("task-456"));
            assertTrue(output.contains("RUNNING"));
            assertTrue(output.contains("Do something"));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void testPrintConfig() {
        var out = new ByteArrayOutputStream();
        var oldOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("version", "1.0.0");
            config.put("provider", "openai");
            OutputFormatter.printConfig(config);
            var output = out.toString();
            assertTrue(output.contains("version: 1.0.0"));
            assertTrue(output.contains("provider: openai"));
        } finally {
            System.setOut(oldOut);
        }
    }
}
