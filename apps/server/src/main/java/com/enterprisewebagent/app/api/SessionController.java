package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptContext;
import com.enterprisewebagent.runtime.prompt.PromptPrecedence;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.query.TurnRequest;
import com.enterprisewebagent.runtime.query.TurnResult;
import com.enterprisewebagent.runtime.session.Session;
import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final SessionManager sessionManager;
    private final TurnEngine turnEngine;
    private final PromptAssembler promptAssembler;
    private final ToolRegistry toolRegistry;

    public SessionController(SessionManager sessionManager,
                             TurnEngine turnEngine,
                             PromptAssembler promptAssembler,
                             ToolRegistry toolRegistry) {
        this.sessionManager = sessionManager;
        this.turnEngine = turnEngine;
        this.promptAssembler = promptAssembler;
        this.toolRegistry = toolRegistry;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, String> body) {
        String workspaceId = body.getOrDefault("workspaceId", "default");
        Session session = sessionManager.create(workspaceId);
        log.info("Session created id={} workspaceId={}", session.id(), workspaceId);
        return ResponseEntity.ok(sessionToMap(session));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        return sessionManager.get(sessionId)
                .map(s -> ResponseEntity.ok(sessionToMap(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{sessionId}/turns")
    public ResponseEntity<Map<String, Object>> executeTurn(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        String input = body.get("input");
        String model = body.get("model");
        if (input == null || input.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "input is required"));
        }

        var sessionOpt = sessionManager.get(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        log.info("Turn requested sessionId={} model={}", sessionId, model);

        PromptContext promptCtx = new PromptContext(sessionId, PromptPrecedence.DEFAULT, Map.of());
        List<PromptSection> promptSections = promptAssembler.assemble(promptCtx);

        ToolContext toolCtx = new ToolContext(sessionId, "normal", Set.of());
        List<ToolDefinition> tools = toolRegistry.resolveTools(toolCtx);

        TurnRequest turnReq = new TurnRequest(sessionId, input, promptSections, tools,
                List.of(), model, Map.of());
        TurnResult result = turnEngine.executeTurn(turnReq);

        return ResponseEntity.ok(Map.of(
                "sessionId", result.sessionId(),
                "output", result.output(),
                "completed", result.completed(),
                "toolCalls", result.toolCalls().size()
        ));
    }

    @PostMapping("/{sessionId}/close")
    public ResponseEntity<Void> closeSession(@PathVariable String sessionId) {
        sessionManager.close(sessionId);
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> sessionToMap(Session session) {
        return Map.of(
                "id", session.id(),
                "workspaceId", session.workspaceId(),
                "status", session.status().name(),
                "created", session.created().toString(),
                "lastActive", session.lastActive().toString()
        );
    }
}
