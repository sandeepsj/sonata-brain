package com.example.sonata_brain.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    List<FeeStructure> findByCourseNameOrderByNameAsc(String courseName);

    List<FeeStructure> findAllByOrderByCourseNameAscNameAsc();

    boolean existsByCourseNameAndName(String courseName, String name);

    boolean existsByCourseNameAndNameAndIdNot(String courseName, String name, Long id);
}
