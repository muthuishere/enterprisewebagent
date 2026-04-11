package com.enterprisewebagent.runtime.skills;

import java.util.Map;

public record SkillDefinition(String name, String source, String content, Map<String, String> frontmatter, SkillOrigin origin) {
}
