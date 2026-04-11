package com.enterprisewebagent.runtime.session;

import com.enterprisewebagent.runtime.query.TranscriptEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionExporterTest {

    private SessionExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new SessionExporter();
    }

    @Test
    void exportMarkdown_producesValidMarkdown() {
        var session = new Session("s1", "ws-1",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T01:00:00Z"),
                SessionStatus.ACTIVE,
                List.of(
                        new TranscriptEntry("user", "Hello", Instant.parse("2025-01-01T00:00:00Z")),
                        new TranscriptEntry("assistant", "Hi there!", Instant.parse("2025-01-01T00:00:01Z"))
                ));

        String md = exporter.exportMarkdown(session);
        assertTrue(md.contains("# Session: s1"));
        assertTrue(md.contains("**Workspace:** ws-1"));
        assertTrue(md.contains("**Status:** ACTIVE"));
        assertTrue(md.contains("## Transcript"));
        assertTrue(md.contains("### User"));
        assertTrue(md.contains("Hello"));
        assertTrue(md.contains("### Assistant"));
        assertTrue(md.contains("Hi there!"));
    }

    @Test
    void exportMarkdown_handlesEmptyTranscript() {
        var session = new Session("s1", "ws-1",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                SessionStatus.CLOSED, List.of());

        String md = exporter.exportMarkdown(session);
        assertTrue(md.contains("_No transcript entries_"));
    }

    @Test
    void exportJson_producesValidJson() {
        var session = new Session("s1", "ws-1",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                SessionStatus.ACTIVE,
                List.of(new TranscriptEntry("user", "Test", Instant.parse("2025-01-01T00:00:00Z"))));

        String json = exporter.exportJson(session);
        assertTrue(json.contains("\"id\" : \"s1\""));
        assertTrue(json.contains("\"workspaceId\" : \"ws-1\""));
        assertTrue(json.contains("\"status\" : \"ACTIVE\""));
        assertTrue(json.contains("\"transcript\""));
        assertTrue(json.contains("\"role\" : \"user\""));
    }

    @Test
    void exportJson_handlesEmptyTranscript() {
        var session = new Session("s1", "ws-1",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                SessionStatus.ACTIVE, List.of());

        String json = exporter.exportJson(session);
        assertTrue(json.contains("\"transcript\" : [ ]"));
    }

    @Test
    void exportSummary_includesKeyInfo() {
        var session = new Session("s1", "ws-1",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T01:00:00Z"),
                SessionStatus.ACTIVE,
                List.of(
                        new TranscriptEntry("user", "Fix the bug", Instant.parse("2025-01-01T00:00:00Z")),
                        new TranscriptEntry("assistant", "I found and fixed the null pointer exception in UserService.java", Instant.parse("2025-01-01T00:00:05Z")),
                        new TranscriptEntry("tool", "file_edit applied", Instant.parse("2025-01-01T00:00:10Z"))
                ));

        String summary = exporter.exportSummary(session);
        assertTrue(summary.contains("# Session Summary: s1"));
        assertTrue(summary.contains("Key Responses"));
        assertTrue(summary.contains("Tool Calls"));
    }

    @Test
    void exportSummary_handlesEmptySession() {
        var session = new Session("s1", "ws-1",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                SessionStatus.ACTIVE, List.of());

        String summary = exporter.exportSummary(session);
        assertTrue(summary.contains("_No activity_"));
    }
}
