package com.example.sonata_brain.controller;

import com.example.sonata_brain.dto.dues.FeeDuesTrackerDTO;
import com.example.sonata_brain.dto.dues.DuesSummaryDTO;
import com.example.sonata_brain.model.FeeDuesTracker;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.StudentService;
import com.example.sonata_brain.service.StudentSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dues")
@CrossOrigin(origins = "*")
public class FeeDuesController {

    private static final Logger logger = LoggerFactory.getLogger(FeeDuesController.class);

    @Autowired
    private FeeDuesTrackerService feeDuesTrackerService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentSessionService studentSessionService;

    /**
     * Get all dues for a specific student
     * GET /api/dues/student/{studentId}
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getDuesByStudent(@PathVariable Long studentId) {
        logger.info("Fetching dues for student ID: {}", studentId);

        List<FeeDuesTracker> dues = feeDuesTrackerService.getDuesByStudentId(studentId);
        List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                .map(FeeDuesTrackerDTO::new)
                .collect(Collectors.toList());

        logger.info("Found {} dues entries for student ID: {}", duesDTO.size(), studentId);
        return ResponseEntity.ok(duesDTO);
    }

    /**
     * Get dues summary for a specific student
     * GET /api/dues/summary/{studentId}
     */
    @GetMapping("/summary/{studentId}")
    public ResponseEntity<DuesSummaryDTO> getDuesSummary(@PathVariable Long studentId) {
        logger.info("Fetching dues summary for student ID: {}", studentId);

        Map<String, Object> summaryData = feeDuesTrackerService.getDuesSummary(studentId);
        String studentName = studentService.findById(studentId)
                .map(student -> student.getStudentName())
                .orElse("Unknown Student");

        DuesSummaryDTO summary = new DuesSummaryDTO(studentId, studentName, summaryData);

        logger.info("Generated dues summary for student ID: {}", studentId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get all dues for a specific year
     * GET /api/dues/year/{year}
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getDuesByYear(@PathVariable Integer year) {
        logger.info("Fetching dues for year: {}", year);

        List<FeeDuesTracker> dues = feeDuesTrackerService.getDuesByYear(year);
        List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                .map(FeeDuesTrackerDTO::new)
                .collect(Collectors.toList());

        logger.info("Found {} dues entries for year: {}", duesDTO.size(), year);
        return ResponseEntity.ok(duesDTO);
    }

    /**
     * Get all dues for a specific term
     * GET /api/dues/term/{year}/{termNumber}
     */
    @GetMapping("/term/{year}/{termNumber}")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getDuesByTerm(
            @PathVariable Integer year,
            @PathVariable Integer termNumber) {
        logger.info("Fetching dues for term: {}-{}", year, termNumber);

        List<FeeDuesTracker> dues = feeDuesTrackerService.getDuesByTerm(year, termNumber);
        List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                .map(FeeDuesTrackerDTO::new)
                .collect(Collectors.toList());

        logger.info("Found {} dues entries for term: {}-{}", duesDTO.size(), year, termNumber);
        return ResponseEntity.ok(duesDTO);
    }

    /**
     * Get all dues for a specific course
     * GET /api/dues/course/{courseName}
     */
    @GetMapping("/course/{courseName}")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getDuesByCourse(@PathVariable String courseName) {
        logger.info("Fetching dues for course: {}", courseName);

        List<FeeDuesTracker> dues = feeDuesTrackerService.getDuesByCourse(courseName);
        List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                .map(FeeDuesTrackerDTO::new)
                .collect(Collectors.toList());

        logger.info("Found {} dues entries for course: {}", duesDTO.size(), courseName);
        return ResponseEntity.ok(duesDTO);
    }

    /**
     * Get all dues for a specific session
     * GET /api/dues/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getDuesBySession(@PathVariable Long sessionId) {
        logger.info("Fetching dues for session ID: {}", sessionId);

        try {
            // Get the student session first
            return studentSessionService.findById(sessionId)
                    .map(session -> {
                        // Get dues for this session
                        List<FeeDuesTracker> dues = feeDuesTrackerService.getDuesBySession(session);
                        List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                                .map(FeeDuesTrackerDTO::new)
                                .collect(Collectors.toList());

                        logger.info("Found {} dues entries for session ID: {}", duesDTO.size(), sessionId);
                        return ResponseEntity.ok(duesDTO);
                    })
                    .orElseGet(() -> {
                        logger.warn("Session not found with ID: {}", sessionId);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error fetching dues for session ID: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get dues by status
     * GET /api/dues/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getDuesByStatus(@PathVariable String status) {
        logger.info("Fetching dues for status: {}", status);

        try {
            FeeDuesTracker.DuesStatus duesStatus = FeeDuesTracker.DuesStatus.valueOf(status.toUpperCase());
            List<FeeDuesTracker> dues = feeDuesTrackerService.getDuesByStatus(duesStatus);
            List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                    .map(FeeDuesTrackerDTO::new)
                    .collect(Collectors.toList());

            logger.info("Found {} dues entries for status: {}", duesDTO.size(), status);
            return ResponseEntity.ok(duesDTO);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status provided: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get dues by student and status
     * GET /api/dues/student/{studentId}/status/{status}
     */
    @GetMapping("/student/{studentId}/status/{status}")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getDuesByStudentAndStatus(
            @PathVariable Long studentId,
            @PathVariable String status) {
        logger.info("Fetching dues for student ID: {} with status: {}", studentId, status);

        try {
            FeeDuesTracker.DuesStatus duesStatus = FeeDuesTracker.DuesStatus.valueOf(status.toUpperCase());
            List<FeeDuesTracker> dues = feeDuesTrackerService.getDuesByStudentAndStatus(studentId, duesStatus);
            List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                    .map(FeeDuesTrackerDTO::new)
                    .collect(Collectors.toList());

            logger.info("Found {} dues entries for student ID: {} with status: {}",
                       duesDTO.size(), studentId, status);
            return ResponseEntity.ok(duesDTO);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status provided: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all active dues (not cancelled)
     * GET /api/dues/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getActiveDues() {
        logger.info("Fetching all active dues");

        List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();
        List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                .map(FeeDuesTrackerDTO::new)
                .collect(Collectors.toList());

        logger.info("Found {} active dues entries", duesDTO.size());
        return ResponseEntity.ok(duesDTO);
    }

    /**
     * Get all dues with filtering options
     * GET /api/dues?status=DUE&year=2025&course=MATH
     */
    @GetMapping
    public ResponseEntity<List<FeeDuesTrackerDTO>> getAllDues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String course) {
        logger.info("Fetching dues with filters - status: {}, year: {}, course: {}",
                   status, year, course);

        List<FeeDuesTracker> dues;

        // Apply filters based on provided parameters
        if (status != null && year != null) {
            try {
                FeeDuesTracker.DuesStatus duesStatus = FeeDuesTracker.DuesStatus.valueOf(status.toUpperCase());
                dues = feeDuesTrackerService.getDuesByStatus(duesStatus).stream()
                        .filter(due -> due.getTermYear().equals(year))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (status != null) {
            try {
                FeeDuesTracker.DuesStatus duesStatus = FeeDuesTracker.DuesStatus.valueOf(status.toUpperCase());
                dues = feeDuesTrackerService.getDuesByStatus(duesStatus);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (year != null) {
            dues = feeDuesTrackerService.getDuesByYear(year);
        } else if (course != null) {
            dues = feeDuesTrackerService.getDuesByCourse(course);
        } else {
            dues = feeDuesTrackerService.getActiveDues(); // Default to active dues
        }

        // Apply course filter if provided and not already used as primary filter
        if (course != null && status != null || course != null && year != null) {
            dues = dues.stream()
                    .filter(due -> due.getStudentSession().getCourse().getName().equalsIgnoreCase(course))
                    .collect(Collectors.toList());
        }

        List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                .map(FeeDuesTrackerDTO::new)
                .collect(Collectors.toList());

        logger.info("Found {} dues entries with applied filters", duesDTO.size());
        return ResponseEntity.ok(duesDTO);
    }
}
