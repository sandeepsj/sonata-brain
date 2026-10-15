package com.example.sonata_brain.dues;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FeeDueRepository extends JpaRepository<FeeDue, Long>, JpaSpecificationExecutor<FeeDue> {

    List<FeeDue> findByStudent_AdmissionNo(Long admissionNo);
}
