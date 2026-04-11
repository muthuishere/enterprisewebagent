package com.enterprisewebagent.runtime.provider;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

public class SpringAiModelProvider implements ModelProvider {

    private final ChatModel chatModel;
    private final String providerId;

    public SpringAiModelProvider(ChatModel chatModel, String providerId) {
        this.chatModel = chatModel;
        this.providerId = providerId;
    }

    @Override
    public String complete(ModelRequest request) {
        List<Message> messages = PromptMapper.toMessages(request.prompt(), null);
        Prompt prompt = buildPrompt(messages, request);
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    @Override
    public Flux<String> stream(ModelRequest request) {
        List<Message> messages = PromptMapper.toMessages(request.prompt(), null);
        Prompt prompt = buildPrompt(messages, request);
        return chatModel.stream(prompt)
                .map(response -> {
                    if (response.getResult() != null && response.getResult().getOutput() != null) {
                        String text = response.getResult().getOutput().getText();
                        return text != null ? text : "";
                    }
                    return "";
                })
                .filter(text -> !text.isEmpty());
    }

    public String providerId() {
        return providerId;
    }

    private Prompt buildPrompt(List<Message> messages, ModelRequest request) {
        if (request.temperature() == null && request.maxTokens() == null) {
            return new Prompt(messages);
        }
        var builder = ChatOptions.builder();
        if (request.temperature() != null) {
            builder.temperature(request.temperature());
        }
        if (request.maxTokens() != null) {
            builder.maxTokens(request.maxTokens());
        }
        return new Prompt(messages, builder.build());
    }
}
