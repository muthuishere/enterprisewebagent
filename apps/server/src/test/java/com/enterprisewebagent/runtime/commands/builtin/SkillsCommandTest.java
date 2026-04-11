package com.enterprisewebagent.runtime.commands.builtin;

import com.enterprisewebagent.runtime.commands.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkillsCommandTest {

    private SkillsCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new SkillsCommand();
    }

    @Test
    void listSkills() {
        var ctx = new CommandContext("s1", "/skills", List.of(), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.output().contains("Skills"));
    }

    @Test
    void disableAndEnable() {
        var disableCtx = new CommandContext("s1", "/skills disable mySkill", List.of("disable", "mySkill"), null, null, null, Map.of());
        var disableResult = cmd.execute(disableCtx);
        assertTrue(disableResult.success());
        assertTrue(disableResult.output().contains("disabled"));

        var listCtx = new CommandContext("s1", "/skills list", List.of("list"), null, null, null, Map.of());
        var listResult = cmd.execute(listCtx);
        assertTrue(listResult.output().contains("mySkill"));

        var enableCtx = new CommandContext("s1", "/skills enable mySkill", List.of("enable", "mySkill"), null, null, null, Map.of());
        var enableResult = cmd.execute(enableCtx);
        assertTrue(enableResult.success());
        assertTrue(enableResult.output().contains("enabled"));
    }

    @Test
    void disableWithoutName_returnsError() {
        var ctx = new CommandContext("s1", "/skills disable", List.of("disable"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
    }

    @Test
    void unknownSubcommand() {
        var ctx = new CommandContext("s1", "/skills foo", List.of("foo"), null, null, null, Map.of());
        var result = cmd.execute(ctx);
        assertFalse(result.success());
    }
}
