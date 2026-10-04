package com.example.sonata_brain.dues;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FeeDueRepository extends JpaRepository<FeeDue, Long> {

    List<FeeDue> findByStudentId(Long studentId);

    @Query("SELECT DISTINCT d.studentId FROM FeeDue d")
    List<Long> findStudentIds();
}
