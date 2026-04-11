package com.enterprisewebagent.runtime.tools.builtin;

import com.enterprisewebagent.runtime.planning.PlanManager;
import com.enterprisewebagent.runtime.tools.DefaultToolRegistry;

public class PlanningToolRegistrar {

    public static void registerAll(DefaultToolRegistry registry, PlanManager planManager) {
        registry.registerExecutor(new EnterPlanModeTool(planManager));
        registry.registerExecutor(new ExitPlanModeTool(planManager));
    }
}
