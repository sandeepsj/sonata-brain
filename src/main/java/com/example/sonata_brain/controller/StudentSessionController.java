package com.example.sonata_brain.controller;

import com.example.sonata_brain.dto.StudentSessionCreateRequest;
import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/student-sessions")
@Validated
public class StudentSessionController {

    private final StudentSessionRepository studentSessionRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public StudentSessionController(
            StudentSessionRepository studentSessionRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository) {
        this.studentSessionRepository = studentSessionRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @PostMapping
    public ResponseEntity<StudentSession> createSession(@Validated @RequestBody StudentSessionCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Course course = courseRepository.findByCourseName(request.getCourseName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        StudentSession session = new StudentSession();
        session.setStudent(student);
        session.setCourse(course);

        return ResponseEntity.ok(studentSessionRepository.save(session));
    }

    @PatchMapping("/{id}/disjoin")
    public ResponseEntity<StudentSession> updateDisjoiningDate(@PathVariable Long id) {
        StudentSession session = studentSessionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getDateOfDisjoining() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is already disjoined");
        }

        session.setDateOfDisjoining(LocalDateTime.now());
        return ResponseEntity.ok(studentSessionRepository.save(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentSession> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(studentSessionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")));
    }
}
