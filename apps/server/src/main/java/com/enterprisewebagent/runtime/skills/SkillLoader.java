package com.enterprisewebagent.runtime.skills;

import java.util.List;

public interface SkillLoader {
    List<SkillDefinition> loadSkills(SkillLoadContext context);
}
