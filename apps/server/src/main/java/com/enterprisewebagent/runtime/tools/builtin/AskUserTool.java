package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.tools.ToolContext;
import com.enterprisewebagent.runtime.tools.ToolExecutor;
import com.enterprisewebagent.runtime.tools.ToolInvocation;
import com.enterprisewebagent.runtime.tools.ToolResult;

import java.util.List;
import java.util.Map;

public class AskUserTool implements ToolExecutor {

    @Override
    public String toolName() {
        return "ask_user";
    }

    @Override
    public ToolResult execute(ToolInvocation invocation, ToolContext context) {
        Map<String, Object> args = invocation.arguments();
        Object questionObj = args.get("question");
        if (questionObj == null) {
            return new ToolResult(toolName(), "Missing required parameter: question", false);
        }

        String question = questionObj.toString();
        StringBuilder output = new StringBuilder("PENDING_USER_INPUT: ").append(question);

        Object choicesObj = args.get("choices");
        if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
            output.append("\nChoices: ").append(choices);
        }

        return new ToolResult(toolName(), output.toString(), true);
    }
}
