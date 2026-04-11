package com.enterprisewebagent.runtime.provider;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.query.TranscriptEntry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptMapperTest {

    @Test
    void toMessagesCombinesSectionsIntoSystemMessage() {
        List<PromptSection> sections = List.of(
                new PromptSection("core", "You are helpful.", false),
                new PromptSection("extra", "Be concise.", false)
        );

        List<Message> messages = PromptMapper.toMessages(sections, null);

        assertEquals(1, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertTrue(messages.get(0).getText().contains("You are helpful."));
        assertTrue(messages.get(0).getText().contains("Be concise."));
    }

    @Test
    void toMessagesAddsUserInput() {
        List<PromptSection> sections = List.of(
                new PromptSection("core", "System prompt.", false)
        );

        List<Message> messages = PromptMapper.toMessages(sections, "Hello!");

        assertEquals(2, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals(MessageType.USER, messages.get(1).getMessageType());
        assertEquals("Hello!", messages.get(1).getText());
    }

    @Test
    void toMessagesSkipsSystemMessageWhenSectionsEmpty() {
        List<Message> messages = PromptMapper.toMessages(List.of(), "Hello!");

        assertEquals(1, messages.size());
        assertEquals(MessageType.USER, messages.get(0).getMessageType());
    }

    @Test
    void toMessagesSkipsBothWhenEmpty() {
        List<Message> messages = PromptMapper.toMessages(List.of(), null);
        assertTrue(messages.isEmpty());
    }

    @Test
    void toMessagesWithTranscriptMapsRolesCorrectly() {
        List<PromptSection> sections = List.of(
                new PromptSection("core", "System prompt.", false)
        );
        List<TranscriptEntry> transcript = List.of(
                new TranscriptEntry("user", "What is 2+2?", Instant.now()),
                new TranscriptEntry("assistant", "4", Instant.now()),
                new TranscriptEntry("tool", "calculator result", Instant.now())
        );

        List<Message> messages = PromptMapper.toMessages(sections, "Follow up", transcript);

        assertEquals(4, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals(MessageType.USER, messages.get(1).getMessageType());
        assertEquals("What is 2+2?", messages.get(1).getText());
        assertEquals(MessageType.ASSISTANT, messages.get(2).getMessageType());
        assertEquals("4", messages.get(2).getText());
        // tool entry skipped, current user input last
        assertEquals(MessageType.USER, messages.get(3).getMessageType());
        assertEquals("Follow up", messages.get(3).getText());
    }

    @Test
    void toMessagesWithTranscriptAndNoUserInput() {
        List<PromptSection> sections = List.of(
                new PromptSection("core", "System prompt.", false)
        );
        List<TranscriptEntry> transcript = List.of(
                new TranscriptEntry("user", "Hi", Instant.now())
        );

        List<Message> messages = PromptMapper.toMessages(sections, null, transcript);

        assertEquals(2, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals(MessageType.USER, messages.get(1).getMessageType());
    }
}
