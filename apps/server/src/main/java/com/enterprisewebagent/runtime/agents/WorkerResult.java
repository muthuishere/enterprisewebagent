package com.enterprisewebagent.runtime.agents;

import com.enterprisewebagent.runtime.tools.ToolInvocation;

import java.util.List;

public record WorkerResult(String workerId, String output, boolean success, List<ToolInvocation> toolActivity) {
}
