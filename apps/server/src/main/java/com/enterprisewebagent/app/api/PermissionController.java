package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.permissions.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final AtomicReference<PermissionMode> currentMode;
    private final CopyOnWriteArrayList<PermissionRule> rules;
    private final DenialTracker denialTracker;

    public PermissionController(AtomicReference<PermissionMode> permissionMode,
                                 CopyOnWriteArrayList<PermissionRule> permissionRules,
                                 DenialTracker denialTracker) {
        this.currentMode = permissionMode;
        this.rules = permissionRules;
        this.denialTracker = denialTracker;
    }

    @GetMapping("/mode")
    public ResponseEntity<Map<String, String>> getMode() {
        return ResponseEntity.ok(Map.of("mode", currentMode.get().name()));
    }

    @PutMapping("/mode")
    public ResponseEntity<Map<String, String>> setMode(@RequestBody Map<String, String> body) {
        String modeName = body.get("mode");
        if (modeName == null || modeName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "mode is required"));
        }
        try {
            PermissionMode mode = PermissionMode.valueOf(modeName.toUpperCase());
            currentMode.set(mode);
            return ResponseEntity.ok(Map.of("mode", mode.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode: " + modeName));
        }
    }

    @GetMapping("/rules")
    public ResponseEntity<List<Map<String, String>>> getRules() {
        List<Map<String, String>> result = rules.stream()
            .map(r -> Map.of(
                "id", r.id(),
                "type", r.type().name(),
                "scope", r.scope().name(),
                "pattern", r.pattern(),
                "description", r.description()
            ))
            .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rules")
    public ResponseEntity<Map<String, String>> addRule(@RequestBody Map<String, String> body) {
        try {
            PermissionRule rule = new PermissionRule(
                body.getOrDefault("id", "rule-" + (rules.size() + 1)),
                PermissionType.valueOf(body.get("type").toUpperCase()),
                PermissionScope.valueOf(body.get("scope").toUpperCase()),
                body.get("pattern"),
                body.getOrDefault("description", "")
            );
            rules.add(rule);
            return ResponseEntity.ok(Map.of("id", rule.id(), "status", "added"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/denials")
    public ResponseEntity<List<Map<String, Object>>> getDenials(
            @RequestParam(defaultValue = "50") int count) {
        List<Map<String, Object>> result = denialTracker.getRecent(count).stream()
            .map(d -> Map.<String, Object>of(
                "toolName", d.toolName(),
                "reason", d.reason(),
                "timestamp", d.timestamp().toString()
            ))
            .toList();
        return ResponseEntity.ok(result);
    }
}
