package com.example.sonata_brain.service;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.exception.StudentNotFoundException;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.spec.StudentSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        
        // Handle admission number - either validate if provided or auto-generate
        if (student.getAdmissionNo() != null) {
            // User provided admission number - check if it's already used
            if (studentRepository.existsByAdmissionNo(student.getAdmissionNo())) {
                throw ApiException.conflict("Admission number " + student.getAdmissionNo() + " is already in use");
            }
        } else {
            // Auto-generate admission number
            Long maxAdmissionNo = studentRepository.findMaxAdmissionNo().orElse(0L);
            student.setAdmissionNo(maxAdmissionNo + 1);
        }
        
        return studentRepository.save(student);
    }

    @Override
    public List<Student> searchStudents(String name, LocalDate admissionDateFrom, LocalDate admissionDateTo,
                                      String school, Integer pinCode,
                                      String contactNo, String className, Long admissionNo) {
        return studentRepository.findAll(
            StudentSpecifications.withDynamicQuery(
                name, admissionDateFrom, admissionDateTo, school, pinCode, contactNo, className, admissionNo
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

    @Override
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    public Page<Student> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }
}
