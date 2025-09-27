package com.example.sonata_brain.repository;

import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.model.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    boolean existsByCourseAndYear(Course course, Integer year);

    List<FeeStructure> findByYear(Integer year);

    List<FeeStructure> findByCourseAndYear(Course course, Integer year);

    Optional<FeeStructure> findByCourseAndYearAndTerm(Course course, Integer year, Integer term);

    List<FeeStructure> findByCourse(Course course);
}
