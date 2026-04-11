package com.enterprisewebagent.runtime.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultSkillLoader implements SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(DefaultSkillLoader.class);

    @Override
    public List<SkillDefinition> loadSkills(SkillLoadContext context) {
        List<SkillDefinition> allSkills = new ArrayList<>();

        if (context.projectRoot() != null) {
            allSkills.addAll(loadFromDirectory(context.projectRoot(), SkillOrigin.PROJECT));
        }

        if (context.userRoot() != null) {
            allSkills.addAll(loadFromDirectory(context.userRoot(), SkillOrigin.USER));
        }

        if (context.managedRoot() != null) {
            allSkills.addAll(loadFromDirectory(context.managedRoot(), SkillOrigin.MANAGED));
        }

        if (!context.enabledSkills().isEmpty()) {
            allSkills.removeIf(s -> !context.enabledSkills().contains(s.name()));
        }

        allSkills.removeIf(s -> context.disabledSkills().contains(s.name()));

        Map<String, SkillDefinition> seen = new LinkedHashMap<>();
        for (SkillDefinition skill : allSkills) {
            seen.putIfAbsent(skill.name(), skill);
        }

        log.info("Skills loaded count={} enabled={} disabled={}",
                seen.size(), context.enabledSkills().size(), context.disabledSkills().size());

        return List.copyOf(seen.values());
    }

    private List<SkillDefinition> loadFromDirectory(Path dir, SkillOrigin origin) {
        return SkillFileScanner.scan(dir).stream()
            .map(path -> loadSingleSkill(path, origin))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    private Optional<SkillDefinition> loadSingleSkill(Path path, SkillOrigin origin) {
        try {
            String raw = Files.readString(path);
            var parsed = FrontmatterParser.parse(raw);
            String name = parsed.frontmatter().getOrDefault("name",
                path.getFileName().toString().replace(".md", ""));
            log.debug("Skill loaded name={} origin={} path={}", name, origin, path);
            return Optional.of(new SkillDefinition(
                name, path.toString(), parsed.content(), parsed.frontmatter(), origin
            ));
        } catch (IOException e) {
            log.warn("Skill load failed path={} error={}", path, e.getMessage());
            return Optional.empty();
        }
    }
}
