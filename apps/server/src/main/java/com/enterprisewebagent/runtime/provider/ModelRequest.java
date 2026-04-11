package com.enterprisewebagent.runtime.provider;

import com.enterprisewebagent.runtime.prompt.PromptSection;

import java.util.List;
import java.util.Map;

public record ModelRequest(List<PromptSection> prompt, String model, Map<String, Object> options) {
}
