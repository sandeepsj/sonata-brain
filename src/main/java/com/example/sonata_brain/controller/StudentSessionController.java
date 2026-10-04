package com.example.sonata_brain.controller;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.course.CourseService;
import com.example.sonata_brain.dto.StudentSessionCreateRequest;
import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import com.example.sonata_brain.service.StudentSessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Enrolment of a student in a course (REQ-030 to REQ-033). */
@RestController
@RequestMapping("/api/student-sessions")
@Validated
public class StudentSessionController {

    public record DisjoinRequest(LocalDateTime date) {
    }

    private final StudentSessionRepository sessions;
    private final StudentRepository students;
    private final CourseRepository courses;
    private final CourseService courseService;
    private final StudentSessionService sessionService;

    public StudentSessionController(StudentSessionRepository sessions, StudentRepository students,
                                    CourseRepository courses, CourseService courseService,
                                    StudentSessionService sessionService) {
        this.sessions = sessions;
        this.students = students;
        this.courses = courses;
        this.courseService = courseService;
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentSession createSession(@Validated @RequestBody StudentSessionCreateRequest request) {
        Student student = students.findById(request.getStudentId())
                .orElseThrow(() -> ApiException.notFound("Student " + request.getStudentId() + " not found"));
        Course course = courseService.requireActive(request.getCourseName());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime joined = request.getDateOfJoining() != null ? request.getDateOfJoining() : now;
        LocalDateTime left = request.getDateOfDisjoining();
        if (joined.isAfter(now)) {
            throw ApiException.invalidField("dateOfJoining", "Date of joining cannot be in the future");
        }
        if (left != null && left.isAfter(now)) {
            throw ApiException.invalidField("dateOfDisjoining", "Date of disjoining cannot be in the future");
        }
        if (left != null && left.isBefore(joined)) {
            throw ApiException.invalidField("dateOfDisjoining", "Date of disjoining cannot be before date of joining");
        }

        List<StudentSession> overlapping = sessions.findOverlappingSessions(
                student, course, joined, left != null ? left : now.plusYears(100));
        if (!overlapping.isEmpty()) {
            StudentSession other = overlapping.get(0);
            throw ApiException.conflict(String.format("Session overlaps with existing session (ID: %d) from %s to %s",
                    other.getId(), other.getDateOfJoining(),
                    other.getDateOfDisjoining() != null ? other.getDateOfDisjoining() : "ongoing"));
        }
        return sessionService.createStudentSession(StudentSession.createSession(student, course, joined, left));
    }

    /** Ends a session now, or at a given past date (REQ-031). */
    @PatchMapping("/{id}/disjoin")
    public StudentSession disjoin(@PathVariable Long id, @RequestBody(required = false) DisjoinRequest request) {
        StudentSession session = find(id);
        if (session.getDateOfDisjoining() != null) {
            throw ApiException.conflict("Session " + id + " is already disjoined");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date = request != null && request.date() != null ? request.date() : now;
        if (date.isAfter(now)) {
            throw ApiException.invalidField("date", "Disjoining date cannot be in the future");
        }
        if (date.isBefore(session.getDateOfJoining())) {
            throw ApiException.invalidField("date", "Disjoining date cannot be before the date of joining");
        }
        return sessionService.disjoinStudent(id, date);
    }

    @GetMapping("/{id}")
    public StudentSession getSession(@PathVariable Long id) {
        return find(id);
    }

    /**
     * Sessions filtered by any combination of student, course, active flag and a date they cover (REQ-032.AC7).
     * An unknown student or course is 404; otherwise an empty list when nothing matches.
     */
    @GetMapping
    public List<StudentSession> list(@RequestParam(required = false) Long studentId,
                                     @RequestParam(required = false) String courseName,
                                     @RequestParam(required = false) Boolean active,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                     LocalDate activeOn) {
        if (studentId != null && !students.existsById(studentId)) {
            throw ApiException.notFound("Student " + studentId + " not found");
        }
        if (courseName != null && !courses.existsById(courseName)) {
            throw ApiException.notFound("Course '" + courseName + "' not found");
        }
        LocalDateTime now = LocalDateTime.now();
        Stream<StudentSession> found = sessions.findAll().stream();
        if (studentId != null) {
            found = found.filter(s -> s.getStudent().getAdmissionNo().equals(studentId));
        }
        if (courseName != null) {
            found = found.filter(s -> s.getCourse().getName().equals(courseName));
        }
        if (active != null) {
            found = found.filter(s -> isActive(s, now) == active);
        }
        if (activeOn != null) {
            found = found.filter(s -> covers(s, activeOn));
        }
        return found.sorted(Comparator.comparing(StudentSession::getId)).toList();
    }

    private StudentSession find(Long id) {
        return sessions.findById(id).orElseThrow(() -> ApiException.notFound("Session " + id + " not found"));
    }

    private static boolean isActive(StudentSession s, LocalDateTime now) {
        return s.getDateOfDisjoining() == null || s.getDateOfDisjoining().isAfter(now);
    }

    private static boolean covers(StudentSession s, LocalDate day) {
        boolean started = !s.getDateOfJoining().toLocalDate().isAfter(day);
        boolean notEnded = s.getDateOfDisjoining() == null || !s.getDateOfDisjoining().toLocalDate().isBefore(day);
        return started && notEnded;
    }
}
