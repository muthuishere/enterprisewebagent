package com.enterprisewebagent.runtime.skills;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSkillLoaderTest {

    @TempDir
    Path tempDir;

    private final DefaultSkillLoader loader = new DefaultSkillLoader();

    @Test
    void loadFromProjectDir() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("project"));
        Files.writeString(projectDir.resolve("greet.md"), """
                ---
                name: greet
                description: Greeting skill
                ---
                Say hello to the user.
                """);

        var context = new SkillLoadContext(projectDir, null);
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(1, skills.size());
        assertEquals("greet", skills.get(0).name());
        assertEquals(SkillOrigin.PROJECT, skills.get(0).origin());
        assertTrue(skills.get(0).content().contains("Say hello"));
    }

    @Test
    void loadFromUserDir() throws IOException {
        Path userDir = Files.createDirectory(tempDir.resolve("user"));
        Files.writeString(userDir.resolve("helper.md"), """
                ---
                name: helper
                ---
                Help the user.
                """);

        var context = new SkillLoadContext(null, userDir);
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(1, skills.size());
        assertEquals("helper", skills.get(0).name());
        assertEquals(SkillOrigin.USER, skills.get(0).origin());
    }

    @Test
    void deduplicationProjectWinsOverUser() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("project"));
        Path userDir = Files.createDirectory(tempDir.resolve("user"));

        Files.writeString(projectDir.resolve("shared.md"), """
                ---
                name: shared
                ---
                Project version.
                """);
        Files.writeString(userDir.resolve("shared.md"), """
                ---
                name: shared
                ---
                User version.
                """);

        var context = new SkillLoadContext(projectDir, userDir);
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(1, skills.size());
        assertEquals(SkillOrigin.PROJECT, skills.get(0).origin());
        assertTrue(skills.get(0).content().contains("Project version."));
    }

    @Test
    void enabledSkillsFilter() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("project"));
        Files.writeString(projectDir.resolve("alpha.md"), """
                ---
                name: alpha
                ---
                Alpha content.
                """);
        Files.writeString(projectDir.resolve("beta.md"), """
                ---
                name: beta
                ---
                Beta content.
                """);

        var context = new SkillLoadContext(projectDir, null, null, Set.of("alpha"), Set.of());
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(1, skills.size());
        assertEquals("alpha", skills.get(0).name());
    }

    @Test
    void disabledSkillsFilter() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("project"));
        Files.writeString(projectDir.resolve("alpha.md"), """
                ---
                name: alpha
                ---
                Alpha content.
                """);
        Files.writeString(projectDir.resolve("beta.md"), """
                ---
                name: beta
                ---
                Beta content.
                """);

        var context = new SkillLoadContext(projectDir, null, null, Set.of(), Set.of("beta"));
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(1, skills.size());
        assertEquals("alpha", skills.get(0).name());
    }

    @Test
    void deterministicOrdering() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("project"));
        Files.writeString(projectDir.resolve("charlie.md"), """
                ---
                name: charlie
                ---
                C.
                """);
        Files.writeString(projectDir.resolve("alpha.md"), """
                ---
                name: alpha
                ---
                A.
                """);
        Files.writeString(projectDir.resolve("bravo.md"), """
                ---
                name: bravo
                ---
                B.
                """);

        var context = new SkillLoadContext(projectDir, null);
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(3, skills.size());
        assertEquals("alpha", skills.get(0).name());
        assertEquals("bravo", skills.get(1).name());
        assertEquals("charlie", skills.get(2).name());
    }

    @Test
    void nameDefaultsToFilename() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("project"));
        Files.writeString(projectDir.resolve("my-skill.md"), "No frontmatter, just content.\n");

        var context = new SkillLoadContext(projectDir, null);
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(1, skills.size());
        assertEquals("my-skill", skills.get(0).name());
    }

    @Test
    void loadFromManagedDir() throws IOException {
        Path managedDir = Files.createDirectory(tempDir.resolve("managed"));
        Files.writeString(managedDir.resolve("builtin.md"), """
                ---
                name: builtin
                ---
                Built-in skill.
                """);

        var context = new SkillLoadContext(null, null, managedDir, Set.of(), Set.of());
        List<SkillDefinition> skills = loader.loadSkills(context);

        assertEquals(1, skills.size());
        assertEquals("builtin", skills.get(0).name());
        assertEquals(SkillOrigin.MANAGED, skills.get(0).origin());
    }

    @Test
    void allNullDirectories() {
        var context = new SkillLoadContext(null, null);
        List<SkillDefinition> skills = loader.loadSkills(context);
        assertTrue(skills.isEmpty());
    }
}
