package com.enterprisewebagent.runtime.agents;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentDefinitionFactoryTest {

    @Test
    void generalWorker_hasCorrectRoleAndTools() {
        var def = AgentDefinitionFactory.generalWorker("gw-1", "general prompt");

        assertEquals("gw-1", def.id());
        assertEquals(AgentRole.GENERAL_WORKER, def.role());
        assertEquals("general prompt", def.promptSurface());
        assertEquals(Set.of("file_read", "file_edit", "shell", "ask_user"), def.allowedTools());
    }

    @Test
    void exploreWorker_isReadOnly() {
        var def = AgentDefinitionFactory.exploreWorker("ew-1", "explore prompt");

        assertEquals(AgentRole.EXPLORE_WORKER, def.role());
        assertEquals(Set.of("file_read", "shell"), def.allowedTools());
        assertFalse(def.allowedTools().contains("file_edit"));
    }

    @Test
    void planWorker_hasCorrectRoleAndTools() {
        var def = AgentDefinitionFactory.planWorker("pw-1", "plan prompt");

        assertEquals(AgentRole.PLAN_WORKER, def.role());
        assertEquals(Set.of("file_read", "shell", "ask_user"), def.allowedTools());
        assertFalse(def.allowedTools().contains("file_edit"));
    }

    @Test
    void verifyWorker_isReadOnly() {
        var def = AgentDefinitionFactory.verifyWorker("vw-1", "verify prompt");

        assertEquals(AgentRole.VERIFY_WORKER, def.role());
        assertEquals(Set.of("file_read", "shell"), def.allowedTools());
        assertFalse(def.allowedTools().contains("file_edit"));
        assertFalse(def.allowedTools().contains("ask_user"));
    }

    @Test
    void coordinator_hasAllToolsIncludingDelegateAndStop() {
        var def = AgentDefinitionFactory.coordinator("coord-1", "coord prompt");

        assertEquals(AgentRole.COORDINATOR, def.role());
        assertEquals(Set.of("file_read", "file_edit", "shell", "ask_user", "worker_delegate", "task_stop"),
            def.allowedTools());
    }
}
