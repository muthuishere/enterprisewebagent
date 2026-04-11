package com.enterprisewebagent.runtime.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionTagStore {

    private final ConcurrentHashMap<String, Set<SessionTag>> tagsBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> sessionsByTag = new ConcurrentHashMap<>();

    public void addTag(String sessionId, String tag) {
        var sessionTag = new SessionTag(sessionId, tag, Instant.now());

        tagsBySession.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionTag);

        sessionsByTag.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public List<String> getTags(String sessionId) {
        Set<SessionTag> tags = tagsBySession.get(sessionId);
        if (tags == null) return List.of();
        return tags.stream().map(SessionTag::tag).toList();
    }

    public List<String> getSessionsByTag(String tag) {
        Set<String> sessionIds = sessionsByTag.get(tag);
        if (sessionIds == null) return List.of();
        return new ArrayList<>(sessionIds);
    }

    public void removeTag(String sessionId, String tag) {
        Set<SessionTag> tags = tagsBySession.get(sessionId);
        if (tags != null) {
            tags.removeIf(t -> t.tag().equals(tag));
        }

        Set<String> sessions = sessionsByTag.get(tag);
        if (sessions != null) {
            sessions.remove(sessionId);
        }
    }
}
