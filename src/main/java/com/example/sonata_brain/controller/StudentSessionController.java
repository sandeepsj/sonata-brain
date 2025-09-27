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
import java.util.List;
import java.util.Optional;

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

        Course course = courseRepository.findByName(request.getCourseName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Validate dates if provided
        LocalDateTime joiningDate = request.getDateOfJoining();
        LocalDateTime disjoinDate = request.getDateOfDisjoining();
        LocalDateTime now = LocalDateTime.now();

        // Validate joining date is not in future
        if (joiningDate != null && joiningDate.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Date of joining cannot be in the future");
        }

        // Validate disjoining date is not in future
        if (disjoinDate != null && disjoinDate.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Date of disjoining cannot be in the future");
        }

        // Check for existing active session
        Optional<StudentSession> existingSession = studentSessionRepository.findActiveSession(student, course);
        if (existingSession.isPresent()) {
            StudentSession activeSession = existingSession.get();
            String errorMessage = activeSession.getDateOfDisjoining() == null
                ? "Student already has an active session for this course"
                : String.format("Student has an active session for this course until %s",
                    activeSession.getDateOfDisjoining().toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        try {
            StudentSession session = StudentSession.createSession(student, course, joiningDate, disjoinDate);
            return ResponseEntity.ok(studentSessionRepository.save(session));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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

    @GetMapping("/active/student/{studentId}")
    public ResponseEntity<List<StudentSession>> getActiveSessionsByStudent(@PathVariable Long studentId) {
        // Verify student exists first
        studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<StudentSession> sessions = studentSessionRepository.findActiveSessionsByStudentId(studentId);
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active sessions found for student ID: " + studentId);
        }
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/active/course/{courseName}")
    public ResponseEntity<List<StudentSession>> getActiveSessionsByCourse(@PathVariable String courseName) {
        // Verify course exists first
        courseRepository.findByName(courseName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<StudentSession> sessions = studentSessionRepository.findActiveSessionsByCourseName(courseName);
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active sessions found for course: " + courseName);
        }
        return ResponseEntity.ok(sessions);
    }
}
