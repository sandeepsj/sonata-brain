package com.example.sonata_brain.service;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.exception.StudentNotFoundException;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.spec.StudentSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Autowired
    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public Student createStudent(Student student) {
        Objects.requireNonNull(student.getStudentName(), "Student name cannot be null");
        return studentRepository.save(student);
    }

    @Override
    public List<Student> searchStudents(String name, LocalDate admissionDate,
                                      String school, Integer pinCode,
                                      String contactNo, String className) {
        return studentRepository.findAll(
            StudentSpecifications.withDynamicQuery(
                name, admissionDate, school, pinCode, contactNo, className
            )
        );
    }

    @Override
    @Transactional
    public Student updateStudent(Long id, Student updatedStudent) {
        Student existingStudent = studentRepository.findById(id)
            .orElseThrow(() -> new StudentNotFoundException(id));

        if (updatedStudent.getStudentName() != null && !updatedStudent.getStudentName().trim().isEmpty()) {
            existingStudent.setStudentName(updatedStudent.getStudentName());
        }
        if (updatedStudent.getContactNo() != null) {
            existingStudent.setContactNo(updatedStudent.getContactNo());
        }
        if (updatedStudent.getDob() != null) {
            existingStudent.setDob(updatedStudent.getDob());
        }
        if (updatedStudent.getClassName() != null) {
            existingStudent.setClassName(updatedStudent.getClassName());
        }
        if (updatedStudent.getSchool() != null) {
            existingStudent.setSchool(updatedStudent.getSchool());
        }
        if (updatedStudent.getAddress() != null) {
            existingStudent.setAddress(updatedStudent.getAddress());
        }
        if (updatedStudent.getPinCode() != null) {
            existingStudent.setPinCode(updatedStudent.getPinCode());
        }
        if (updatedStudent.getRemarks() != null) {
            existingStudent.setRemarks(updatedStudent.getRemarks());
        }
        if (updatedStudent.getStatus() != null) {
            existingStudent.setStatus(updatedStudent.getStatus());
        }

        return studentRepository.save(existingStudent);
    }

    @Override
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
            .orElseThrow(() -> new StudentNotFoundException(id));
    }
}
