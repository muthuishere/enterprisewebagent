package com.enterprisewebagent.runtime.memory;

import com.enterprisewebagent.runtime.query.TurnResult;

public interface SessionMemory {
    String getMemoryPrompt();
    void onTurnComplete(TurnResult result);
}
