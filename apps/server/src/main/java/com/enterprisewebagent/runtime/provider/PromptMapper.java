package com.enterprisewebagent.runtime.provider;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.query.TranscriptEntry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PromptMapper {

    public static List<Message> toMessages(List<PromptSection> sections, String userInput) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = sections.stream()
                .map(PromptSection::content)
                .collect(Collectors.joining("\n\n"));

        if (!systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        if (userInput != null && !userInput.isBlank()) {
            messages.add(new UserMessage(userInput));
        }

        return messages;
    }

    public static List<Message> toMessages(List<PromptSection> sections, String userInput,
                                           List<TranscriptEntry> transcript) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = sections.stream()
                .map(PromptSection::content)
                .collect(Collectors.joining("\n\n"));

        if (!systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        for (TranscriptEntry entry : transcript) {
            switch (entry.role()) {
                case "user" -> messages.add(new UserMessage(entry.content()));
                case "assistant" -> messages.add(new AssistantMessage(entry.content()));
                default -> { /* tool/system entries skipped */ }
            }
        }

        if (userInput != null && !userInput.isBlank()) {
            messages.add(new UserMessage(userInput));
        }

        return messages;
    }
}
