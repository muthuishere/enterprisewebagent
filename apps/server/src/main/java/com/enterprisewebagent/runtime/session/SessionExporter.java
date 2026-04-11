package com.enterprisewebagent.runtime.session;

import com.enterprisewebagent.runtime.query.TranscriptEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SessionExporter {

    private final ObjectMapper objectMapper;

    public SessionExporter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public SessionExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String exportMarkdown(Session session) {
        var sb = new StringBuilder();
        sb.append("# Session: ").append(session.id()).append("\n\n");
        sb.append("- **Workspace:** ").append(session.workspaceId()).append("\n");
        sb.append("- **Created:** ").append(session.created()).append("\n");
        sb.append("- **Last Active:** ").append(session.lastActive()).append("\n");
        sb.append("- **Status:** ").append(session.status()).append("\n\n");
        sb.append("## Transcript\n\n");

        List<TranscriptEntry> transcript = session.transcript();
        if (transcript == null || transcript.isEmpty()) {
            sb.append("_No transcript entries_\n");
        } else {
            for (TranscriptEntry entry : transcript) {
                sb.append("### ").append(capitalize(entry.role()));
                sb.append(" (").append(entry.timestamp()).append(")\n\n");
                sb.append(entry.content()).append("\n\n");
                sb.append("---\n\n");
            }
        }

        return sb.toString();
    }

    public String exportJson(Session session) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", session.id());
            data.put("workspaceId", session.workspaceId());
            data.put("created", session.created().toString());
            data.put("lastActive", session.lastActive().toString());
            data.put("status", session.status().name());
            data.put("transcript", session.transcript().stream()
                    .map(e -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("role", e.role());
                        entry.put("content", e.content());
                        entry.put("timestamp", e.timestamp().toString());
                        return entry;
                    })
                    .toList());

            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to export: " + e.getMessage() + "\"}";
        }
    }

    public String exportSummary(Session session) {
        var sb = new StringBuilder();
        sb.append("# Session Summary: ").append(session.id()).append("\n\n");
        sb.append("**Status:** ").append(session.status()).append("\n");
        sb.append("**Duration:** ").append(session.created()).append(" → ").append(session.lastActive()).append("\n\n");

        List<TranscriptEntry> transcript = session.transcript();
        if (transcript == null || transcript.isEmpty()) {
            sb.append("_No activity_\n");
            return sb.toString();
        }

        // Extract key actions from assistant responses
        List<TranscriptEntry> assistantEntries = transcript.stream()
                .filter(e -> "assistant".equals(e.role()))
                .toList();

        List<TranscriptEntry> toolEntries = transcript.stream()
                .filter(e -> "tool".equals(e.role()) || "tool_result".equals(e.role()))
                .toList();

        if (!assistantEntries.isEmpty()) {
            sb.append("## Key Responses (").append(assistantEntries.size()).append(")\n\n");
            for (TranscriptEntry entry : assistantEntries) {
                String summary = entry.content().length() > 150
                        ? entry.content().substring(0, 150) + "..."
                        : entry.content();
                sb.append("- ").append(summary).append("\n");
            }
            sb.append("\n");
        }

        if (!toolEntries.isEmpty()) {
            sb.append("## Tool Calls (").append(toolEntries.size()).append(")\n\n");
            for (TranscriptEntry entry : toolEntries) {
                String summary = entry.content().length() > 100
                        ? entry.content().substring(0, 100) + "..."
                        : entry.content();
                sb.append("- ").append(summary).append("\n");
            }
        }

        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
