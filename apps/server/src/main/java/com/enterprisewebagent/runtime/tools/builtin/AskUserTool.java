package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.events.AskUserRequestedEvent;
import com.enterprisewebagent.runtime.events.RuntimeEventPublisher;
import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.List;
import java.util.Map;

public class AskUserTool implements ToolExecutor {

    private final RuntimeEventPublisher eventPublisher;

    public AskUserTool(RuntimeEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String toolName() {
        return "ask_user";
    }

    @SuppressWarnings("unchecked")
    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();
        Object questionObj = args.get("question");
        if (questionObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: question", false);
        }

        String question = questionObj.toString();
        StringBuilder output = new StringBuilder("PENDING_USER_INPUT: ").append(question);

        List<String> choicesList = List.of();
        Object choicesObj = args.get("choices");
        if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
            choicesList = choices.stream().map(Object::toString).toList();
            output.append("\nChoices: ").append(choicesList);
        }

        eventPublisher.publish(new AskUserRequestedEvent(context.sessionId(), question, choicesList));

        return new ToolResult(toolName(), output.toString(), true);
    }
}
