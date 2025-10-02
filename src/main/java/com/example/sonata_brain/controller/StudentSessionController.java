package com.example.sonata_brain.controller;

import com.example.sonata_brain.dto.StudentSessionCreateRequest;
import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import com.example.sonata_brain.service.StudentSessionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/student-sessions")
@Validated
public class StudentSessionController {

    private final StudentSessionRepository studentSessionRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudentSessionService studentSessionService;

    @Autowired
    public StudentSessionController(
            StudentSessionRepository studentSessionRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            StudentSessionService studentSessionService) {
        this.studentSessionRepository = studentSessionRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.studentSessionService = studentSessionService;
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

        // Set defaults if not provided
        if (joiningDate == null) {
            joiningDate = now;
        }

        // If no disjoin date provided, set a future date for overlap checking
        LocalDateTime endDateForCheck = disjoinDate != null ? disjoinDate : now.plusYears(100);

        // Validate joining date is not in future
        if (joiningDate.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Date of joining cannot be in the future");
        }

        // Validate disjoining date is not in future
        if (disjoinDate != null && disjoinDate.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Date of disjoining cannot be in the future");
        }

        // Validate disjoining date is after joining date
        if (disjoinDate != null && disjoinDate.isBefore(joiningDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Date of disjoining cannot be before date of joining");
        }

        // Check for overlapping sessions
        List<StudentSession> overlappingSessions = studentSessionRepository.findOverlappingSessions(
            student, course, joiningDate, endDateForCheck);

        if (!overlappingSessions.isEmpty()) {
            StudentSession conflictSession = overlappingSessions.get(0);
            String conflictMessage = String.format(
                "Session overlaps with existing session (ID: %d) from %s to %s",
                conflictSession.getId(),
                conflictSession.getDateOfJoining().toString(),
                conflictSession.getDateOfDisjoining() != null
                    ? conflictSession.getDateOfDisjoining().toString()
                    : "ongoing"
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, conflictMessage);
        }

        try {
            StudentSession session = StudentSession.createSession(student, course, joiningDate, disjoinDate);
            // Use service instead of direct repository save to trigger events
            return ResponseEntity.ok(studentSessionService.createStudentSession(session));
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

        // Use service to trigger events for dues recalculation
        return ResponseEntity.ok(studentSessionService.disjoinStudent(id, LocalDateTime.now()));
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

    @GetMapping("/all/student/{studentId}")
    public ResponseEntity<List<StudentSession>> getAllSessionsByStudent(@PathVariable Long studentId) {
        // Verify student exists first
        studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<StudentSession> sessions = studentSessionRepository.findAllSessionsByStudentId(studentId);
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No sessions found for student ID: " + studentId);
        }
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/all/course/{courseName}")
    public ResponseEntity<List<StudentSession>> getAllSessionsByCourse(@PathVariable String courseName) {
        // Verify course exists first
        courseRepository.findByName(courseName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<StudentSession> sessions = studentSessionRepository.findAllSessionsByCourseName(courseName);
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No sessions found for course: " + courseName);
        }
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/all")
    public ResponseEntity<List<StudentSession>> getAllSessions(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Boolean active) {

        List<StudentSession> sessions;

        if (studentId != null && courseName != null) {
            // Both student and course specified
            studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
            courseRepository.findByName(courseName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

            if (active != null) {
                sessions = active
                    ? studentSessionRepository.findActiveSessionsByStudentIdAndCourseName(studentId, courseName)
                    : studentSessionRepository.findInactiveSessionsByStudentIdAndCourseName(studentId, courseName);
            } else {
                sessions = studentSessionRepository.findAllSessionsByStudentIdAndCourseName(studentId, courseName);
            }
        } else if (studentId != null) {
            // Only student specified
            studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

            if (active != null) {
                sessions = active
                    ? studentSessionRepository.findActiveSessionsByStudentId(studentId)
                    : studentSessionRepository.findInactiveSessionsByStudentId(studentId);
            } else {
                sessions = studentSessionRepository.findAllSessionsByStudentId(studentId);
            }
        } else if (courseName != null) {
            // Only course specified
            courseRepository.findByName(courseName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

            if (active != null) {
                sessions = active
                    ? studentSessionRepository.findActiveSessionsByCourseName(courseName)
                    : studentSessionRepository.findInactiveSessionsByCourseName(courseName);
            } else {
                sessions = studentSessionRepository.findAllSessionsByCourseName(courseName);
            }
        } else {
            // No specific filters, get all sessions
            if (active != null) {
                sessions = active
                    ? studentSessionRepository.findAllActiveSessions()
                    : studentSessionRepository.findAllInactiveSessions();
            } else {
                sessions = studentSessionRepository.findAll();
            }
        }

        return ResponseEntity.ok(sessions);
    }
}
