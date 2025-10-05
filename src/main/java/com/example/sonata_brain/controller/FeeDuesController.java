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
     * Get all dues with comprehensive filtering options
     * GET /api/dues?statuses=DUE,PARTIALLY_PAID&year=2025&course=MATH&termNumber=3
     *
     * Query Parameters:
     * - statuses: Comma-separated list of statuses (DUE, PARTIALLY_PAID, PAID, CANCELLED)
     * - year: Filter by term year
     * - termNumber: Filter by term number
     * - course: Filter by course name
     *
     * All parameters are optional and can be combined in any way
     */
    @GetMapping
    public ResponseEntity<List<FeeDuesTrackerDTO>> getAllDues(
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer termNumber,
            @RequestParam(required = false) String course) {
        logger.info("Fetching dues with filters - statuses: {}, year: {}, termNumber: {}, course: {}",
                   statuses, year, termNumber, course);

        try {
            // Start with all active dues if no filters provided
            List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();

            // Apply status filter
            if (statuses != null && !statuses.trim().isEmpty()) {
                String[] statusArray = statuses.split(",");
                List<FeeDuesTracker.DuesStatus> statusList = new java.util.ArrayList<>();

                for (String statusStr : statusArray) {
                    try {
                        statusList.add(FeeDuesTracker.DuesStatus.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid status provided: {}", statusStr);
                        return ResponseEntity.badRequest().build();
                    }
                }

                dues = dues.stream()
                        .filter(due -> statusList.contains(due.getStatus()))
                        .collect(Collectors.toList());
            }

            // Apply year filter
            if (year != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermYear().equals(year))
                        .collect(Collectors.toList());
            }

            // Apply term number filter
            if (termNumber != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermNumber().equals(termNumber))
                        .collect(Collectors.toList());
            }

            // Apply course filter
            if (course != null) {
                dues = dues.stream()
                        .filter(due -> due.getStudentSession().getCourse().getName().equalsIgnoreCase(course))
                        .collect(Collectors.toList());
            }

            List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                    .map(FeeDuesTrackerDTO::new)
                    .collect(Collectors.toList());

            logger.info("Found {} dues entries with applied filters", duesDTO.size());
            return ResponseEntity.ok(duesDTO);
        } catch (Exception e) {
            logger.error("Error fetching dues with filters", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get dues that are either DUE or PARTIALLY_PAID (convenience endpoint)
     * GET /api/dues/pending?year=2025&course=MATH&termNumber=3
     */
    @GetMapping("/pending")
    public ResponseEntity<List<FeeDuesTrackerDTO>> getPendingDues(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer termNumber,
            @RequestParam(required = false) String course) {
        logger.info("Fetching pending dues with filters - year: {}, termNumber: {}, course: {}",
                   year, termNumber, course);

        try {
            List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();

            // Filter for DUE or PARTIALLY_PAID status
            dues = dues.stream()
                    .filter(due -> due.getStatus() == FeeDuesTracker.DuesStatus.DUE ||
                                 due.getStatus() == FeeDuesTracker.DuesStatus.PARTIALLY_PAID)
                    .collect(Collectors.toList());

            // Apply year filter
            if (year != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermYear().equals(year))
                        .collect(Collectors.toList());
            }

            // Apply term number filter
            if (termNumber != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermNumber().equals(termNumber))
                        .collect(Collectors.toList());
            }

            // Apply course filter
            if (course != null) {
                dues = dues.stream()
                        .filter(due -> due.getStudentSession().getCourse().getName().equalsIgnoreCase(course))
                        .collect(Collectors.toList());
            }

            List<FeeDuesTrackerDTO> duesDTO = dues.stream()
                    .map(FeeDuesTrackerDTO::new)
                    .collect(Collectors.toList());

            logger.info("Found {} pending dues entries with applied filters", duesDTO.size());
            return ResponseEntity.ok(duesDTO);
        } catch (Exception e) {
            logger.error("Error fetching pending dues with filters", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get aggregate statistics for dues (count and total amount)
     * GET /api/dues/aggregate?statuses=DUE,PARTIALLY_PAID&year=2024&course=VOCAL&termNumber=3
     *
     * Query Parameters:
     * - statuses: Comma-separated list of statuses (DUE, PARTIALLY_PAID, PAID, CANCELLED)
     * - year: Filter by term year
     * - termNumber: Filter by term number
     * - course: Filter by course name
     *
     * Returns: { "count": 5, "totalDue": 1250.00, "filters": {...} }
     */
    @GetMapping("/aggregate")
    public ResponseEntity<Map<String, Object>> getDuesAggregate(
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer termNumber,
            @RequestParam(required = false) String course) {
        logger.info("Fetching dues aggregate with filters - statuses: {}, year: {}, termNumber: {}, course: {}",
                   statuses, year, termNumber, course);

        try {
            // Start with all active dues
            List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();

            // Apply status filter
            if (statuses != null && !statuses.trim().isEmpty()) {
                String[] statusArray = statuses.split(",");
                List<FeeDuesTracker.DuesStatus> statusList = new java.util.ArrayList<>();

                for (String statusStr : statusArray) {
                    try {
                        statusList.add(FeeDuesTracker.DuesStatus.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid status provided: {}", statusStr);
                        return ResponseEntity.badRequest().build();
                    }
                }

                dues = dues.stream()
                        .filter(due -> statusList.contains(due.getStatus()))
                        .collect(Collectors.toList());
            }

            // Apply year filter
            if (year != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermYear().equals(year))
                        .collect(Collectors.toList());
            }

            // Apply term number filter
            if (termNumber != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermNumber().equals(termNumber))
                        .collect(Collectors.toList());
            }

            // Apply course filter
            if (course != null) {
                dues = dues.stream()
                        .filter(due -> due.getStudentSession().getCourse().getName().equalsIgnoreCase(course))
                        .collect(Collectors.toList());
            }

            // Calculate aggregates
            long count = dues.size();
            double totalDue = dues.stream()
                    .mapToDouble(due -> due.getDueAmount().doubleValue())
                    .sum();

            // Build response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("count", count);
            response.put("totalDue", totalDue);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (statuses != null) filters.put("statuses", statuses);
            if (year != null) filters.put("year", year);
            if (termNumber != null) filters.put("termNumber", termNumber);
            if (course != null) filters.put("course", course);
            response.put("filters", filters);

            logger.info("Aggregate result: {} dues with total amount due: {}", count, totalDue);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching dues aggregate", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get aggregate statistics for pending dues (DUE or PARTIALLY_PAID)
     * GET /api/dues/aggregate/pending?year=2024&course=VOCAL&termNumber=3
     *
     * Returns: { "count": 3, "totalDue": 750.00, "filters": {...} }
     */
    @GetMapping("/aggregate/pending")
    public ResponseEntity<Map<String, Object>> getPendingDuesAggregate(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer termNumber,
            @RequestParam(required = false) String course) {
        logger.info("Fetching pending dues aggregate with filters - year: {}, termNumber: {}, course: {}",
                   year, termNumber, course);

        try {
            List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();

            // Filter for DUE or PARTIALLY_PAID status
            dues = dues.stream()
                    .filter(due -> due.getStatus() == FeeDuesTracker.DuesStatus.DUE ||
                                 due.getStatus() == FeeDuesTracker.DuesStatus.PARTIALLY_PAID)
                    .collect(Collectors.toList());

            // Apply year filter
            if (year != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermYear().equals(year))
                        .collect(Collectors.toList());
            }

            // Apply term number filter
            if (termNumber != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermNumber().equals(termNumber))
                        .collect(Collectors.toList());
            }

            // Apply course filter
            if (course != null) {
                dues = dues.stream()
                        .filter(due -> due.getStudentSession().getCourse().getName().equalsIgnoreCase(course))
                        .collect(Collectors.toList());
            }

            // Calculate aggregates
            long count = dues.size();
            double totalDue = dues.stream()
                    .mapToDouble(due -> due.getDueAmount().doubleValue())
                    .sum();

            // Build response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("count", count);
            response.put("totalDue", totalDue);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (year != null) filters.put("year", year);
            if (termNumber != null) filters.put("termNumber", termNumber);
            if (course != null) filters.put("course", course);
            response.put("filters", filters);

            logger.info("Pending dues aggregate result: {} dues with total amount due: {}", count, totalDue);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching pending dues aggregate", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get aggregate statistics grouped by course
     * GET /api/dues/aggregate/by-course?year=2024&statuses=DUE,PARTIALLY_PAID
     *
     * Returns: [
     *   { "course": "VOCAL", "count": 5, "totalDue": 1250.00 },
     *   { "course": "PIANO", "count": 3, "totalDue": 900.00 }
     * ]
     */
    @GetMapping("/aggregate/by-course")
    public ResponseEntity<List<Map<String, Object>>> getDuesAggregateGroupedByCourse(
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer termNumber) {
        logger.info("Fetching dues aggregate grouped by course - statuses: {}, year: {}, termNumber: {}",
                   statuses, year, termNumber);

        try {
            List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();

            // Apply status filter
            if (statuses != null && !statuses.trim().isEmpty()) {
                String[] statusArray = statuses.split(",");
                List<FeeDuesTracker.DuesStatus> statusList = new java.util.ArrayList<>();

                for (String statusStr : statusArray) {
                    try {
                        statusList.add(FeeDuesTracker.DuesStatus.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid status provided: {}", statusStr);
                        return ResponseEntity.badRequest().build();
                    }
                }

                dues = dues.stream()
                        .filter(due -> statusList.contains(due.getStatus()))
                        .collect(Collectors.toList());
            }

            // Apply year filter
            if (year != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermYear().equals(year))
                        .collect(Collectors.toList());
            }

            // Apply term number filter
            if (termNumber != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermNumber().equals(termNumber))
                        .collect(Collectors.toList());
            }

            // Group by course and calculate aggregates
            Map<String, List<FeeDuesTracker>> groupedByCourse = dues.stream()
                    .collect(Collectors.groupingBy(due -> due.getStudentSession().getCourse().getName()));

            List<Map<String, Object>> results = groupedByCourse.entrySet().stream()
                    .map(entry -> {
                        String courseName = entry.getKey();
                        List<FeeDuesTracker> courseDues = entry.getValue();

                        long count = courseDues.size();
                        double totalDue = courseDues.stream()
                                .mapToDouble(due -> due.getDueAmount().doubleValue())
                                .sum();

                        Map<String, Object> courseAggregate = new java.util.HashMap<>();
                        courseAggregate.put("course", courseName);
                        courseAggregate.put("count", count);
                        courseAggregate.put("totalDue", totalDue);

                        return courseAggregate;
                    })
                    .collect(Collectors.toList());

            logger.info("Grouped by course: {} courses found", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error fetching dues aggregate grouped by course", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get aggregate statistics grouped by year
     * GET /api/dues/aggregate/by-year?statuses=DUE,PARTIALLY_PAID&course=VOCAL
     *
     * Returns: [
     *   { "year": 2024, "count": 8, "totalDue": 2000.00 },
     *   { "year": 2025, "count": 4, "totalDue": 1200.00 }
     * ]
     */
    @GetMapping("/aggregate/by-year")
    public ResponseEntity<List<Map<String, Object>>> getDuesAggregateGroupedByYear(
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Integer termNumber) {
        logger.info("Fetching dues aggregate grouped by year - statuses: {}, course: {}, termNumber: {}",
                   statuses, course, termNumber);

        try {
            List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();

            // Apply status filter
            if (statuses != null && !statuses.trim().isEmpty()) {
                String[] statusArray = statuses.split(",");
                List<FeeDuesTracker.DuesStatus> statusList = new java.util.ArrayList<>();

                for (String statusStr : statusArray) {
                    try {
                        statusList.add(FeeDuesTracker.DuesStatus.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid status provided: {}", statusStr);
                        return ResponseEntity.badRequest().build();
                    }
                }

                dues = dues.stream()
                        .filter(due -> statusList.contains(due.getStatus()))
                        .collect(Collectors.toList());
            }

            // Apply course filter
            if (course != null) {
                dues = dues.stream()
                        .filter(due -> due.getStudentSession().getCourse().getName().equalsIgnoreCase(course))
                        .collect(Collectors.toList());
            }

            // Apply term number filter
            if (termNumber != null) {
                dues = dues.stream()
                        .filter(due -> due.getTermNumber().equals(termNumber))
                        .collect(Collectors.toList());
            }

            // Group by year and calculate aggregates
            Map<Integer, List<FeeDuesTracker>> groupedByYear = dues.stream()
                    .collect(Collectors.groupingBy(FeeDuesTracker::getTermYear));

            List<Map<String, Object>> results = groupedByYear.entrySet().stream()
                    .map(entry -> {
                        Integer year = entry.getKey();
                        List<FeeDuesTracker> yearDues = entry.getValue();

                        long count = yearDues.size();
                        double totalDue = yearDues.stream()
                                .mapToDouble(due -> due.getDueAmount().doubleValue())
                                .sum();

                        Map<String, Object> yearAggregate = new java.util.HashMap<>();
                        yearAggregate.put("year", year);
                        yearAggregate.put("count", count);
                        yearAggregate.put("totalDue", totalDue);

                        return yearAggregate;
                    })
                    .sorted((a, b) -> ((Integer)a.get("year")).compareTo((Integer)b.get("year")))
                    .collect(Collectors.toList());

            logger.info("Grouped by year: {} years found", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error fetching dues aggregate grouped by year", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get aggregate statistics grouped by term (year + term number)
     * GET /api/dues/aggregate/by-term?statuses=DUE,PARTIALLY_PAID&course=VOCAL
     *
     * Returns: [
     *   { "year": 2024, "termNumber": 1, "count": 3, "totalDue": 675.00 },
     *   { "year": 2024, "termNumber": 2, "count": 5, "totalDue": 1125.00 }
     * ]
     */
    @GetMapping("/aggregate/by-term")
    public ResponseEntity<List<Map<String, Object>>> getDuesAggregateGroupedByTerm(
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) String course) {
        logger.info("Fetching dues aggregate grouped by term - statuses: {}, course: {}",
                   statuses, course);

        try {
            List<FeeDuesTracker> dues = feeDuesTrackerService.getActiveDues();

            // Apply status filter
            if (statuses != null && !statuses.trim().isEmpty()) {
                String[] statusArray = statuses.split(",");
                List<FeeDuesTracker.DuesStatus> statusList = new java.util.ArrayList<>();

                for (String statusStr : statusArray) {
                    try {
                        statusList.add(FeeDuesTracker.DuesStatus.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid status provided: {}", statusStr);
                        return ResponseEntity.badRequest().build();
                    }
                }

                dues = dues.stream()
                        .filter(due -> statusList.contains(due.getStatus()))
                        .collect(Collectors.toList());
            }

            // Apply course filter
            if (course != null) {
                dues = dues.stream()
                        .filter(due -> due.getStudentSession().getCourse().getName().equalsIgnoreCase(course))
                        .collect(Collectors.toList());
            }

            // Group by year and term number
            Map<String, List<FeeDuesTracker>> groupedByTerm = dues.stream()
                    .collect(Collectors.groupingBy(due -> due.getTermYear() + "-" + due.getTermNumber()));

            List<Map<String, Object>> results = groupedByTerm.entrySet().stream()
                    .map(entry -> {
                        String[] parts = entry.getKey().split("-");
                        Integer year = Integer.parseInt(parts[0]);
                        Integer termNumber = Integer.parseInt(parts[1]);
                        List<FeeDuesTracker> termDues = entry.getValue();

                        long count = termDues.size();
                        double totalDue = termDues.stream()
                                .mapToDouble(due -> due.getDueAmount().doubleValue())
                                .sum();

                        Map<String, Object> termAggregate = new java.util.HashMap<>();
                        termAggregate.put("year", year);
                        termAggregate.put("termNumber", termNumber);
                        termAggregate.put("count", count);
                        termAggregate.put("totalDue", totalDue);

                        return termAggregate;
                    })
                    .sorted((a, b) -> {
                        int yearCompare = ((Integer)a.get("year")).compareTo((Integer)b.get("year"));
                        if (yearCompare != 0) return yearCompare;
                        return ((Integer)a.get("termNumber")).compareTo((Integer)b.get("termNumber"));
                    })
                    .collect(Collectors.toList());

            logger.info("Grouped by term: {} terms found", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error fetching dues aggregate grouped by term", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Mark a due as STUDENT_LEFT_DUE_CANCELLED
     * PATCH /api/dues/{duesId}/mark-student-left-cancelled
     *
     * This API is used when a student has left and their remaining dues should be marked as cancelled
     * due to student departure. This is different from regular cancellation.
     */
    @PatchMapping("/{duesId}/mark-student-left-cancelled")
    public ResponseEntity<?> markDueAsStudentLeftCancelled(@PathVariable Long duesId) {
        logger.info("Marking due ID {} as STUDENT_LEFT_DUE_CANCELLED", duesId);

        try {
            FeeDuesTracker updatedDue = feeDuesTrackerService.markDueAsStudentLeftCancelled(duesId);
            FeeDuesTrackerDTO dueDTO = new FeeDuesTrackerDTO(updatedDue);

            logger.info("Successfully marked due ID {} as STUDENT_LEFT_DUE_CANCELLED", duesId);
            return ResponseEntity.ok(dueDTO);
        } catch (IllegalArgumentException e) {
            logger.error("Error marking due as student left cancelled: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid Request",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error marking due ID {} as student left cancelled", duesId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal Server Error",
                "message", "An error occurred while marking the due as student left cancelled"
            ));
        }
    }
}
