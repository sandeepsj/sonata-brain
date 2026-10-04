package com.example.sonata_brain.dues;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DueWaiverRepository extends JpaRepository<DueWaiver, Long> {

    List<DueWaiver> findBySessionIdIn(Collection<Long> sessionIds);

    boolean existsBySessionIdAndPeriodStart(Long sessionId, LocalDate periodStart);
}
