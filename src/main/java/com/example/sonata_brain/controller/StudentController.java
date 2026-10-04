package com.example.sonata_brain.controller;

import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<Page<Student>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Student> students = studentService.getAllStudents(pageable);
        return ResponseEntity.ok(students);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@RequestBody Student student) {
        if (student.getStudentName() == null || student.getStudentName().trim().isEmpty()) {
            throw ApiException.invalidField("studentName", "Student name is required");
        }
        if (student.getAdmissionDate() == null) {
            student.setAdmissionDate(LocalDate.now());
        }
        return studentService.createStudent(student);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Student>> searchStudents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate admissionDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate admissionDateTo,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) Integer pinCode,
            @RequestParam(required = false) String contactNo,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) Long admissionNo) {

        List<Student> students = studentService.searchStudents(
                name, admissionDateFrom, admissionDateTo, school, pinCode, contactNo, className, admissionNo
        );
        return ResponseEntity.ok(students);
    }

    @PutMapping("/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return studentService.updateStudent(id, student);
    }

    @GetMapping("/{id}")
    public Student getStudent(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }
}
