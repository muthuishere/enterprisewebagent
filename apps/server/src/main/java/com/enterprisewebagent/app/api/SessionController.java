package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandDispatcher;
import com.enterprisewebagent.runtime.planning.*;
import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptContext;
import com.enterprisewebagent.runtime.prompt.PromptPrecedence;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.query.TurnRequest;
import com.enterprisewebagent.runtime.query.TurnResult;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.session.Session;
import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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
    private final PlanManager planManager;
    private final CommandDispatcher commandDispatcher;
    private final TaskManager taskManager;
    private final DefaultModelProviderRegistry modelProviderRegistry;

    public SessionController(SessionManager sessionManager,
                             TurnEngine turnEngine,
                             PromptAssembler promptAssembler,
                             ToolRegistry toolRegistry,
                             PlanManager planManager,
                             CommandDispatcher commandDispatcher,
                             TaskManager taskManager,
                             DefaultModelProviderRegistry modelProviderRegistry) {
        this.sessionManager = sessionManager;
        this.turnEngine = turnEngine;
        this.promptAssembler = promptAssembler;
        this.toolRegistry = toolRegistry;
        this.planManager = planManager;
        this.commandDispatcher = commandDispatcher;
        this.taskManager = taskManager;
        this.modelProviderRegistry = modelProviderRegistry;
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

        // Slash command interception
        if (commandDispatcher.isCommand(input)) {
            var cmdCtx = new CommandContext(sessionId, input, List.of(),
                    sessionManager, taskManager, modelProviderRegistry);
            var cmdResult = commandDispatcher.dispatch(input, cmdCtx);
            if (cmdResult.suppressTurn()) {
                var response = new LinkedHashMap<String, Object>();
                response.put("sessionId", sessionId);
                response.put("output", cmdResult.output());
                response.put("completed", true);
                response.put("toolCalls", 0);
                response.put("isCommand", true);
                return ResponseEntity.ok(response);
            }
            // If not suppressed, use the command output as new input to the model
            input = cmdResult.output();
        }

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

    // --- Tag & Export endpoints ---

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSessions(@RequestParam(required = false) String tag) {
        if (tag != null && !tag.isBlank()) {
            List<Session> sessions = sessionManager.findByTag(tag);
            List<Map<String, Object>> result = sessions.stream()
                    .map(this::sessionToMap)
                    .toList();
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{sessionId}/tags")
    public ResponseEntity<Map<String, Object>> addTag(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        String tag = body.get("tag");
        if (tag == null || tag.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "tag is required"));
        }
        try {
            sessionManager.tagSession(sessionId, tag);
            var response = new LinkedHashMap<String, Object>();
            response.put("sessionId", sessionId);
            response.put("tag", tag);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{sessionId}/tags")
    public ResponseEntity<Map<String, Object>> getTags(@PathVariable String sessionId) {
        var sessionOpt = sessionManager.get(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<String> tags = sessionManager.getTags(sessionId);
        var response = new LinkedHashMap<String, Object>();
        response.put("sessionId", sessionId);
        response.put("tags", tags);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}/export")
    public ResponseEntity<Map<String, Object>> exportSession(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "markdown") String format) {
        try {
            String exported = sessionManager.exportSession(sessionId, format);
            var response = new LinkedHashMap<String, Object>();
            response.put("sessionId", sessionId);
            response.put("format", format);
            response.put("content", exported);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Plan Mode endpoints ---

    @PostMapping("/{sessionId}/plan")
    public ResponseEntity<Map<String, Object>> createOrGetPlan(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        if (sessionManager.get(sessionId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var existing = planManager.getPlan(sessionId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(planToMap(existing.get()));
        }

        String goal = body.getOrDefault("goal", "");
        if (goal.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "goal is required"));
        }

        Plan plan = planManager.createPlan(sessionId, goal);
        log.info("Plan created sessionId={} goal={}", sessionId, goal);
        return ResponseEntity.ok(planToMap(plan));
    }

    @GetMapping("/{sessionId}/plan")
    public ResponseEntity<Map<String, Object>> getPlan(@PathVariable String sessionId) {
        return planManager.getPlan(sessionId)
                .map(plan -> ResponseEntity.ok(planToMap(plan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{sessionId}/plan/steps/{stepId}")
    public ResponseEntity<Map<String, Object>> updatePlanStep(
            @PathVariable String sessionId,
            @PathVariable String stepId,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        PlanStepStatus status;
        try {
            status = PlanStepStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + statusStr));
        }

        try {
            Plan plan = planManager.updateStepStatus(sessionId, stepId, status);
            return ResponseEntity.ok(planToMap(plan));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/plan/execute")
    public ResponseEntity<Map<String, Object>> executePlan(@PathVariable String sessionId) {
        var planOpt = planManager.getPlan(sessionId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Plan plan = planManager.setMode(sessionId, PlanMode.EXECUTING);
        log.info("Plan execution started sessionId={}", sessionId);
        return ResponseEntity.ok(planToMap(plan));
    }

    private Map<String, Object> planToMap(Plan plan) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionId", plan.sessionId());
        map.put("goal", plan.goal());
        map.put("mode", plan.mode().name());
        map.put("created", plan.created().toString());
        map.put("steps", plan.steps().stream().map(step -> {
            Map<String, Object> stepMap = new LinkedHashMap<>();
            stepMap.put("id", step.id());
            stepMap.put("description", step.description());
            stepMap.put("status", step.status().name());
            if (step.result() != null) {
                stepMap.put("result", step.result());
            }
            return stepMap;
        }).toList());
        return map;
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
