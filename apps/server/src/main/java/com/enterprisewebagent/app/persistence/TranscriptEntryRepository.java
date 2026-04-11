package com.enterprisewebagent.app.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptEntryRepository extends JpaRepository<TranscriptEntryEntity, Long> {
    List<TranscriptEntryEntity> findBySessionIdOrderBySequenceNum(String sessionId);

    int countBySessionId(String sessionId);
}
