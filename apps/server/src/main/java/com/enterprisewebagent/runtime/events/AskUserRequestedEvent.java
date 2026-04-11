package com.enterprisewebagent.runtime.events;

import java.util.List;

public record AskUserRequestedEvent(
    String sessionId,
    String question,
    List<String> choices
) implements RuntimeEvent {}
