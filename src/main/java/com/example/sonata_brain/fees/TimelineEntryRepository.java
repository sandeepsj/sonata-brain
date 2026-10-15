package com.example.sonata_brain.fees;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineEntryRepository extends JpaRepository<TimelineEntry, Long> {

    List<TimelineEntry> findAllByOrderByEffectiveFromAsc();

    boolean existsByStructure_Id(Long structureId);
}
