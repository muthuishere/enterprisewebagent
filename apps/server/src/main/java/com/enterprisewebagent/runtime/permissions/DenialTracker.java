package com.enterprisewebagent.runtime.permissions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class DenialTracker {

    private static final int MAX_DENIALS = 1000;

    private final ConcurrentLinkedDeque<Denial> denials = new ConcurrentLinkedDeque<>();

    public void recordDenial(String toolName, String reason, Instant timestamp) {
        denials.addLast(new Denial(toolName, reason, timestamp));
        while (denials.size() > MAX_DENIALS) {
            denials.pollFirst();
        }
    }

    public List<Denial> getRecent(int count) {
        List<Denial> all = List.copyOf(denials);
        int fromIndex = Math.max(0, all.size() - count);
        return all.subList(fromIndex, all.size());
    }

    public Map<String, Long> getDenialCounts() {
        return denials.stream()
            .collect(Collectors.groupingBy(Denial::toolName, Collectors.counting()));
    }

    public int size() {
        return denials.size();
    }
}
