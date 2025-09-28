package com.example.sonata_brain.service;

import com.example.sonata_brain.model.Student;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentService {
    Student createStudent(Student student);
    List<Student> searchStudents(String name, LocalDate admissionDate, String school,
                               Integer pinCode, String contactNo, String className);
    Student updateStudent(Long id, Student student);
    Student getStudentById(Long id);
    Optional<Student> findById(Long id);
}
