package com.dilip.transaction.repository;

import com.dilip.transaction.entity.CuratedTransaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface CuratedRepository
        extends JpaRepository<CuratedTransaction, Long> {

    boolean existsByEventId(String id);

    List<CuratedTransaction> findByRunId(String id);

    @Query("SELECT c.eventId FROM CuratedTransaction c WHERE c.eventId IN :eventIds")
    Set<String> findExistingEventIds(
            @Param("eventIds") Set<String> eventIds);
}