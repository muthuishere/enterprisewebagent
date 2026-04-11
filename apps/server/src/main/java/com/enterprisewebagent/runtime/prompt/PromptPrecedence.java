package com.enterprisewebagent.runtime.prompt;

public enum PromptPrecedence {
    OVERRIDE,
    COORDINATOR,
    CUSTOM_AGENT,
    USER_SYSTEM,
    DEFAULT
}
