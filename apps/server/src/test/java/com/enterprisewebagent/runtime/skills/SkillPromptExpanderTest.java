package com.enterprisewebagent.runtime.skills;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkillPromptExpanderTest {

    @Test
    void expandCreatesPromptSectionPerSkill() {
        var skills = List.of(
            new SkillDefinition("greet", "/path/greet.md", "Say hello.", Map.of("description", "Greeting"), SkillOrigin.PROJECT),
            new SkillDefinition("help", "/path/help.md", "Provide help.", Map.of(), SkillOrigin.USER)
        );

        List<PromptSection> sections = SkillPromptExpander.expand(skills);

        assertEquals(2, sections.size());
    }

    @Test
    void sectionNameIsPrefixed() {
        var skills = List.of(
            new SkillDefinition("my-skill", "/path/my-skill.md", "Content.", Map.of(), SkillOrigin.PROJECT)
        );

        List<PromptSection> sections = SkillPromptExpander.expand(skills);

        assertEquals("skill_my-skill", sections.get(0).name());
    }

    @Test
    void sectionsAreUncached() {
        var skills = List.of(
            new SkillDefinition("test", "/path/test.md", "Content.", Map.of(), SkillOrigin.PROJECT)
        );

        List<PromptSection> sections = SkillPromptExpander.expand(skills);

        assertFalse(sections.get(0).cached());
    }

    @Test
    void sectionContentIncludesDescription() {
        var skills = List.of(
            new SkillDefinition("greet", "/path/greet.md", "Body text.", Map.of("description", "A greeting skill"), SkillOrigin.PROJECT)
        );

        List<PromptSection> sections = SkillPromptExpander.expand(skills);

        String content = sections.get(0).content();
        assertTrue(content.contains("## Skill: greet"));
        assertTrue(content.contains("A greeting skill"));
        assertTrue(content.contains("Body text."));
    }

    @Test
    void sectionContentWithoutDescription() {
        var skills = List.of(
            new SkillDefinition("plain", "/path/plain.md", "Just body.", Map.of(), SkillOrigin.USER)
        );

        List<PromptSection> sections = SkillPromptExpander.expand(skills);

        String content = sections.get(0).content();
        assertTrue(content.contains("## Skill: plain"));
        assertTrue(content.contains("Just body."));
        assertFalse(content.contains("\n\n\n"));
    }

    @Test
    void expandEmptyList() {
        List<PromptSection> sections = SkillPromptExpander.expand(List.of());
        assertTrue(sections.isEmpty());
    }
}
