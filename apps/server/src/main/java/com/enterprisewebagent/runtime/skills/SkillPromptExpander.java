package com.enterprisewebagent.runtime.skills;

import com.enterprisewebagent.runtime.prompt.PromptSection;

import java.util.List;

public class SkillPromptExpander {

    public static List<PromptSection> expand(List<SkillDefinition> skills) {
        return skills.stream()
            .map(skill -> new PromptSection(
                "skill_" + skill.name(),
                formatSkillContent(skill),
                false
            ))
            .toList();
    }

    private static String formatSkillContent(SkillDefinition skill) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Skill: ").append(skill.name());
        if (skill.frontmatter().containsKey("description")) {
            sb.append("\n").append(skill.frontmatter().get("description"));
        }
        sb.append("\n\n").append(skill.content());
        return sb.toString();
    }
}
