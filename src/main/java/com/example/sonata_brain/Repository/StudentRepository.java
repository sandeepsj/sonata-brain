package com.example.sonata_brain.Repository;

import com.example.sonata_brain.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    // Base methods from JpaRepository and JpaSpecificationExecutor will be used
    
    // Check if admission number already exists
    boolean existsByAdmissionNo(Long admissionNo);
    
    // Find the maximum admission number for auto-generation
    @Query("SELECT MAX(s.admissionNo) FROM Student s")
    Optional<Long> findMaxAdmissionNo();
}
