package com.enterprisewebagent.runtime.session;

import java.util.Optional;

public interface SessionManager {
    Session create(String workspaceId);
    Optional<Session> get(String sessionId);
    Session resume(String sessionId);
    void close(String sessionId);
}
