package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.events.AskUserRequestedEvent;
import com.enterprisewebagent.runtime.events.InMemoryEventPublisher;
import com.enterprisewebagent.runtime.events.RuntimeEvent;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AskUserToolTest {

    private AskUserTool tool;
    private List<RuntimeEvent> publishedEvents;

    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        InMemoryEventPublisher publisher = new InMemoryEventPublisher();
        publisher.addListener(publishedEvents::add);
        tool = new AskUserTool(publisher);
    }

    @Test
    void toolNameIsAskUser() {
        assertEquals("ask_user", tool.toolName());
    }

    @Test
    void missingQuestionReturnsFalse() {
        var invocation = new ToolInvocation("ask_user", Map.of());
        var context = new ToolContext("s1", "NORMAL", Set.of());

        ToolResult result = tool.execute(invocation, context);

        assertFalse(result.success());
        assertTrue(result.output().contains("Missing required parameter"));
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    void publishesEventWithQuestion() {
        var invocation = new ToolInvocation("ask_user", Map.of("question", "What next?"));
        var context = new ToolContext("session-42", "NORMAL", Set.of());

        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertTrue(result.output().contains("PENDING_USER_INPUT"));

        assertEquals(1, publishedEvents.size());
        var event = (AskUserRequestedEvent) publishedEvents.get(0);
        assertEquals("session-42", event.sessionId());
        assertEquals("What next?", event.question());
        assertTrue(event.choices().isEmpty());
    }

    @Test
    void publishesEventWithChoices() {
        var invocation = new ToolInvocation("ask_user",
                Map.of("question", "Pick one", "choices", List.of("A", "B", "C")));
        var context = new ToolContext("s1", "NORMAL", Set.of());

        ToolResult result = tool.execute(invocation, context);

        assertTrue(result.success());
        assertEquals(1, publishedEvents.size());
        var event = (AskUserRequestedEvent) publishedEvents.get(0);
        assertEquals("Pick one", event.question());
        assertEquals(List.of("A", "B", "C"), event.choices());
    }
}
