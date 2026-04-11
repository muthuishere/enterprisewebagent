package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.Command;
import com.enterprisewebagent.runtime.commands.CommandContext;
import com.enterprisewebagent.runtime.commands.CommandResult;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SkillsCommand implements Command {

    private final Map<String, Set<String>> disabledSkills = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "skills";
    }

    @Override
    public String description() {
        return "List/manage loaded skills";
    }

    @Override
    public String usage() {
        return "/skills [list|enable <name>|disable <name>]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String subcommand = context.args().isEmpty() ? "list" : context.args().getFirst();

        return switch (subcommand) {
            case "list" -> {
                var disabled = disabledSkills.getOrDefault(context.sessionId(), Set.of());
                var sb = new StringBuilder("Skills\n");
                sb.append("  Built-in skills are always available.\n");
                if (disabled.isEmpty()) {
                    sb.append("  No skills are currently disabled.");
                } else {
                    sb.append("  Disabled: ").append(String.join(", ", disabled));
                }
                yield CommandResult.success(sb.toString());
            }
            case "enable" -> {
                if (context.args().size() < 2) {
                    yield CommandResult.error("Usage: /skills enable <name>");
                }
                String skillName = context.args().get(1);
                var disabled = disabledSkills.get(context.sessionId());
                if (disabled != null) {
                    disabled.remove(skillName);
                }
                yield CommandResult.success("Skill enabled: " + skillName);
            }
            case "disable" -> {
                if (context.args().size() < 2) {
                    yield CommandResult.error("Usage: /skills disable <name>");
                }
                String skillName = context.args().get(1);
                disabledSkills.computeIfAbsent(context.sessionId(),
                        k -> ConcurrentHashMap.newKeySet()).add(skillName);
                yield CommandResult.success("Skill disabled: " + skillName);
            }
            default -> CommandResult.error("Unknown subcommand: " + subcommand + ". Use list, enable, or disable.");
        };
    }
}
