package com.example.sonata_brain.Repository;

import com.example.sonata_brain.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findAllByStudentName(String studentName);
}

