package com.enterprisewebagent.runtime.skills;

import java.nio.file.Path;
import java.util.Set;

public record SkillLoadContext(
    Path projectRoot,
    Path userRoot,
    Path managedRoot,
    Set<String> enabledSkills,
    Set<String> disabledSkills
) {
    public SkillLoadContext(Path projectRoot, Path userRoot) {
        this(projectRoot, userRoot, null, Set.of(), Set.of());
    }
}
