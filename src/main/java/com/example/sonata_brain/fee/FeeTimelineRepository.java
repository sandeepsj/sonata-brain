package com.example.sonata_brain.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeTimelineRepository extends JpaRepository<FeeTimelineEntry, Long> {

    List<FeeTimelineEntry> findByCourseNameOrderByEffectiveFromAsc(String courseName);

    boolean existsByFeeStructure_Id(Long feeStructureId);

    List<FeeTimelineEntry> findByFeeStructure_Id(Long feeStructureId);
}
