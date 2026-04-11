package com.enterprisewebagent.app.config;

import com.enterprisewebagent.runtime.provider.SpringAiModelProvider;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class ModelProviderConfig {

    @Bean
    @Conditional(OpenAiApiKeyPresent.class)
    public SpringAiModelProvider openaiProvider(ObjectProvider<OpenAiChatModel> chatModelProvider) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return null;
        }
        return new SpringAiModelProvider(chatModel, "openai");
    }

    @Bean
    @Conditional(AnthropicApiKeyPresent.class)
    public SpringAiModelProvider anthropicProvider(ObjectProvider<AnthropicChatModel> chatModelProvider) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return null;
        }
        return new SpringAiModelProvider(chatModel, "anthropic");
    }

    @Bean
    @ConditionalOnProperty(name = "app.providers.ollama.enabled", havingValue = "true")
    public SpringAiModelProvider ollamaProvider(ObjectProvider<OllamaChatModel> chatModelProvider) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) return null;
        return new SpringAiModelProvider(chatModel, "ollama");
    }

    @Bean
    @ConditionalOnProperty(name = "app.providers.copilot.enabled", havingValue = "true")
    public SpringAiModelProvider copilotProvider(CopilotTokenProvider tokenProvider) {
        if (!tokenProvider.isAvailable()) return null;

        var api = OpenAiApi.builder()
            .baseUrl("https://api.githubcopilot.com")
            .apiKey(tokenProvider.getToken())
            .build();
        var model = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder().model("gpt-4.1").build())
            .build();
        return new SpringAiModelProvider(model, "copilot");
    }

    @Bean
    @ConditionalOnProperty(name = "app.providers.codex.enabled", havingValue = "true")
    public SpringAiModelProvider codexProvider(CodexTokenProvider tokenProvider) {
        if (!tokenProvider.isAvailable()) return null;

        var api = OpenAiApi.builder()
            .baseUrl("https://api.openai.com")
            .apiKey(tokenProvider.getToken())
            .build();
        var model = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder().model("o3").build())
            .build();
        return new SpringAiModelProvider(model, "codex");
    }

    static class OpenAiApiKeyPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String key = context.getEnvironment().getProperty("spring.ai.openai.api-key", "");
            return hasValidApiKey(key);
        }
    }

    static class AnthropicApiKeyPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String key = context.getEnvironment().getProperty("spring.ai.anthropic.api-key", "");
            return hasValidApiKey(key);
        }
    }

    private static boolean hasValidApiKey(String key) {
        return key != null && !key.isBlank();
    }
}
