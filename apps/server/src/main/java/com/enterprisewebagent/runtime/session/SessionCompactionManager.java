package com.enterprisewebagent.runtime.session;

import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.prompt.PromptSectionCache;
import com.enterprisewebagent.runtime.query.CompactionStrategy;
import com.enterprisewebagent.runtime.query.PromptBudgetCalculator;
import com.enterprisewebagent.runtime.query.TranscriptCompactor;
import com.enterprisewebagent.runtime.query.TranscriptEntry;

import java.util.List;
import java.util.Optional;

public class SessionCompactionManager {

    private final PromptSectionCache promptCache;

    public SessionCompactionManager(PromptSectionCache promptCache) {
        this.promptCache = promptCache;
    }

    public CompactionResult compact(List<TranscriptEntry> transcript, CompactionStrategy strategy) {
        List<TranscriptEntry> compacted = TranscriptCompactor.compact(transcript, strategy);

        switch (strategy) {
            case CLEAR, TRUNCATE -> promptCache.clear();
            case COMPACT -> promptCache.clearDynamic();
        }

        return new CompactionResult(compacted, strategy, transcript.size(), compacted.size());
    }

    public Optional<CompactionStrategy> detectNeeded(List<PromptSection> promptSections, List<TranscriptEntry> transcript) {
        var budget = PromptBudgetCalculator.checkBudget(promptSections, transcript);

        if (!budget.withinBudget()) {
            return Optional.of(CompactionStrategy.COMPACT);
        }
        if (budget.recommendation().startsWith("APPROACHING")) {
            return Optional.of(CompactionStrategy.COMPACT);
        }
        if (transcript.size() > 200) {
            return Optional.of(CompactionStrategy.COMPACT);
        }
        return Optional.empty();
    }

    public record CompactionResult(
            List<TranscriptEntry> compactedTranscript,
            CompactionStrategy strategyUsed,
            int originalSize,
            int compactedSize
    ) {}
}
