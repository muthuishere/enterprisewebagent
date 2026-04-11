package com.enterprisewebagent.runtime.agents;

import com.enterprisewebagent.runtime.prompt.PromptCatalog;
import com.enterprisewebagent.runtime.prompt.PromptContext;
import com.enterprisewebagent.runtime.prompt.PromptPrecedence;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkerPromptResolverTest {

    @Test
    void resolvePromptCatalog_coordinator() {
        assertEquals(PromptCatalog.COORDINATOR_MODE, WorkerPromptResolver.resolvePromptCatalog(AgentRole.COORDINATOR));
    }

    @Test
    void resolvePromptCatalog_generalWorker() {
        assertEquals(PromptCatalog.WORKER_GENERAL, WorkerPromptResolver.resolvePromptCatalog(AgentRole.GENERAL_WORKER));
    }

    @Test
    void resolvePromptCatalog_exploreWorker() {
        assertEquals(PromptCatalog.WORKER_EXPLORE, WorkerPromptResolver.resolvePromptCatalog(AgentRole.EXPLORE_WORKER));
    }

    @Test
    void resolvePromptCatalog_planWorker() {
        assertEquals(PromptCatalog.WORKER_PLAN, WorkerPromptResolver.resolvePromptCatalog(AgentRole.PLAN_WORKER));
    }

    @Test
    void resolvePromptCatalog_verifyWorker() {
        assertEquals(PromptCatalog.WORKER_VERIFY, WorkerPromptResolver.resolvePromptCatalog(AgentRole.VERIFY_WORKER));
    }

    @Test
    void buildWorkerPromptContext_workerUsesCustomAgentPrecedence() {
        var worker = AgentDefinitionFactory.generalWorker("w1", "do stuff");
        var parent = new AgentContext("sess-1", AgentRole.COORDINATOR, Map.of("key", "val"));

        PromptContext ctx = WorkerPromptResolver.buildWorkerPromptContext(worker, parent, "task desc");

        assertEquals(PromptPrecedence.CUSTOM_AGENT, ctx.activePrecedence());
        assertEquals("sess-1", ctx.sessionId());
        assertNull(ctx.coordinatorPrompt());
        assertEquals("do stuff", ctx.customAgentPrompt());
    }

    @Test
    void buildWorkerPromptContext_coordinatorUsesCoordinatorPrecedence() {
        var coord = AgentDefinitionFactory.coordinator("c1", "coordinate");
        var parent = new AgentContext("sess-2", AgentRole.COORDINATOR, Map.of());

        PromptContext ctx = WorkerPromptResolver.buildWorkerPromptContext(coord, parent, "coord task");

        assertEquals(PromptPrecedence.COORDINATOR, ctx.activePrecedence());
        assertEquals("coordinate", ctx.coordinatorPrompt());
        assertNull(ctx.customAgentPrompt());
    }

    @Test
    void buildWorkerPromptContext_inheritedContextPassedThrough() {
        var worker = AgentDefinitionFactory.exploreWorker("e1", "explore");
        var inherited = Map.of("project", "webapp", "env", "dev");
        var parent = new AgentContext("sess-3", AgentRole.COORDINATOR, inherited);

        PromptContext ctx = WorkerPromptResolver.buildWorkerPromptContext(worker, parent, "search files");

        assertEquals("webapp", ctx.dynamicSections().get("project"));
        assertEquals("dev", ctx.dynamicSections().get("env"));
        assertEquals("search files", ctx.dynamicSections().get("worker_task"));
    }

    @Test
    void buildWorkerPromptContext_doesNotMutateParentContext() {
        var worker = AgentDefinitionFactory.planWorker("p1", "plan");
        var inherited = Map.of("key", "value");
        var parent = new AgentContext("sess-4", AgentRole.COORDINATOR, inherited);

        WorkerPromptResolver.buildWorkerPromptContext(worker, parent, "plan task");

        assertFalse(parent.inheritedContext().containsKey("worker_task"));
    }
}
