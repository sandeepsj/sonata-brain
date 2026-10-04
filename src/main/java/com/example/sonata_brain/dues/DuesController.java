package com.example.sonata_brain.dues;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.access.User;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesEvents.StudentDuesInvalidated;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reading dues, waivers, rebuild jobs and the legacy comparison (REQ-072, REQ-074, REQ-076, REQ-081). */
@RestController
@RequestMapping("/api/dues")
public class DuesController {

    public record DueResponse(Long id, Long studentId, String studentName, Long sessionId, String courseName,
                              LocalDate periodStart, LocalDate periodEnd, int termNumber, int termsPerYear,
                              Long feeStructureId, BigDecimal amount, BigDecimal paidAmount, BigDecimal discountAmount,
                              BigDecimal balance, BigDecimal overpaidAmount, FeeDue.Status status) {
    }

    public record Summary(BigDecimal amount, BigDecimal paid, BigDecimal discount, BigDecimal balance,
                          BigDecimal overpaid, Map<FeeDue.Status, Long> counts) {
    }

    public record Group(String key, long count, BigDecimal amount, BigDecimal balance) {
    }

    public record WaiverRequest(Long sessionId, LocalDate periodStart, String reason) {
    }

    public record WaiverResponse(Long id, Long sessionId, LocalDate periodStart, String reason, String createdBy,
                                 LocalDateTime createdAt) {
        static WaiverResponse of(DueWaiver w) {
            return new WaiverResponse(w.getId(), w.getSessionId(), w.getPeriodStart(), w.getReason(),
                    w.getCreatedBy(), w.getCreatedAt());
        }
    }

    public record JobStarted(Long jobId) {
    }

    public record JobResponse(Long jobId, DuesRebuildJob.Scope scope, String scopeId, DuesRebuildJob.Status status,
                              boolean dryRun, int created, int updated, int deleted, List<Object> errors,
                              List<Object> changes, LocalDateTime startedAt, LocalDateTime finishedAt) {
    }

    public record Schedule(String cron, LocalDateTime nextRunAt) {
    }

    public record StudentComparison(Long studentId, BigDecimal legacyBilled, BigDecimal newBilled,
                                    BigDecimal legacyOutstanding, BigDecimal newOutstanding, boolean match) {
    }

    public record Totals(BigDecimal legacyBilled, BigDecimal newBilled, BigDecimal legacyOutstanding,
                         BigDecimal newOutstanding) {
    }

    public record Comparison(List<StudentComparison> students, Totals totals, long mismatches) {
    }

    public enum GroupBy { course, month, status }

    private final FeeDueRepository dues;
    private final DueWaiverRepository waivers;
    private final DuesRebuildJobRepository jobs;
    private final DuesRebuildService rebuilds;
    private final StudentRepository students;
    private final StudentSessionRepository sessions;
    private final CourseRepository courses;
    private final ApplicationEventPublisher events;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final String nightlyCron;

    public DuesController(FeeDueRepository dues, DueWaiverRepository waivers, DuesRebuildJobRepository jobs,
                          DuesRebuildService rebuilds, StudentRepository students, StudentSessionRepository sessions,
                          CourseRepository courses, ApplicationEventPublisher events, JdbcTemplate jdbc,
                          ObjectMapper json, @Value("${sonata.dues.nightly-cron:0 0 2 * * *}") String nightlyCron) {
        this.dues = dues;
        this.waivers = waivers;
        this.jobs = jobs;
        this.rebuilds = rebuilds;
        this.students = students;
        this.sessions = sessions;
        this.courses = courses;
        this.events = events;
        this.jdbc = jdbc;
        this.json = json;
        this.nightlyCron = nightlyCron;
    }

    // ---------- queries (REQ-076) ----------

    @GetMapping
    @Transactional(readOnly = true)
    public List<DueResponse> list(@RequestParam(required = false) Long studentId,
                                  @RequestParam(required = false) String courseName,
                                  @RequestParam(required = false) FeeDue.Status status,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<FeeDue> found = filtered(studentId, courseName, status, from, to).toList();
        Map<Long, String> names = students.findAllById(found.stream().map(FeeDue::getStudentId).distinct().toList())
                .stream().collect(Collectors.toMap(Student::getAdmissionNo, Student::getStudentName));
        return found.stream()
                .sorted(Comparator.comparing(FeeDue::getStudentId).thenComparing(FeeDue::getPeriodStart)
                        .thenComparing(FeeDue::getSessionId))
                .map(d -> new DueResponse(d.getId(), d.getStudentId(), names.get(d.getStudentId()), d.getSessionId(),
                        d.getCourseName(), d.getPeriodStart(), d.getPeriodEnd(), d.getTermNumber(),
                        d.getTermsPerYear(), d.getFeeStructureId(), d.getAmount(), d.getPaidAmount(),
                        d.getDiscountAmount(), d.getBalance(), d.getOverpaidAmount(), d.getStatus()))
                .toList();
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Summary summary(@RequestParam(required = false) Long studentId,
                           @RequestParam(required = false) String courseName) {
        if (studentId == null && courseName == null) {
            throw ApiException.badRequest("Give studentId or courseName");
        }
        List<FeeDue> found = filtered(studentId, courseName, null, null, null).toList();
        Map<FeeDue.Status, Long> counts = new EnumMap<>(FeeDue.Status.class);
        for (FeeDue.Status s : FeeDue.Status.values()) {
            counts.put(s, found.stream().filter(d -> d.getStatus() == s).count());
        }
        return new Summary(sum(found, FeeDue::getAmount), sum(found, FeeDue::getPaidAmount),
                sum(found, FeeDue::getDiscountAmount), sum(found, FeeDue::getBalance),
                sum(found, FeeDue::getOverpaidAmount), counts);
    }

    @GetMapping("/aggregate")
    @Transactional(readOnly = true)
    public List<Group> aggregate(@RequestParam GroupBy groupBy,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Function<FeeDue, String> key = switch (groupBy) {
            case course -> FeeDue::getCourseName;
            case month -> d -> d.getPeriodStart().toString().substring(0, 7);
            case status -> d -> d.getStatus().name();
        };
        Map<String, List<FeeDue>> groups = filtered(null, null, null, from, to)
                .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()));
        return groups.entrySet().stream()
                .map(e -> new Group(e.getKey(), e.getValue().size(), sum(e.getValue(), FeeDue::getAmount),
                        sum(e.getValue(), FeeDue::getBalance)))
                .toList();
    }

    private Stream<FeeDue> filtered(Long studentId, String courseName, FeeDue.Status status, LocalDate from,
                                    LocalDate to) {
        Stream<FeeDue> s = (studentId != null ? dues.findByStudentId(studentId) : dues.findAll()).stream();
        if (courseName != null) {
            s = s.filter(d -> d.getCourseName().equals(courseName));
        }
        if (status != null) {
            s = s.filter(d -> d.getStatus() == status);
        }
        if (from != null) {
            s = s.filter(d -> !d.getPeriodStart().isBefore(from));
        }
        if (to != null) {
            s = s.filter(d -> !d.getPeriodStart().isAfter(to));
        }
        return s;
    }

    private static BigDecimal sum(List<FeeDue> list, Function<FeeDue, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------- waivers (REQ-072) ----------

    @PostMapping("/waivers")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public WaiverResponse waive(@RequestBody(required = false) WaiverRequest request, @AuthenticationPrincipal User user) {
        if (request == null || request.sessionId() == null || request.periodStart() == null) {
            throw ApiException.badRequest("sessionId and periodStart are required");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.invalidField("reason", "A reason is required");
        }
        StudentSession session = sessions.findById(request.sessionId())
                .orElseThrow(() -> ApiException.notFound("Session " + request.sessionId() + " not found"));
        if (session.getDateOfDisjoining() == null) {
            throw ApiException.conflict("Only a disjoined session's dues can be waived");
        }
        if (waivers.existsBySessionIdAndPeriodStart(session.getId(), request.periodStart())) {
            throw ApiException.conflict("That period is already waived");
        }
        DueWaiver saved = waivers.save(new DueWaiver(session.getId(), request.periodStart(), request.reason().trim(),
                user.getEmail()));
        events.publishEvent(new StudentDuesInvalidated(session.getStudent().getAdmissionNo()));
        return WaiverResponse.of(saved);
    }

    @DeleteMapping("/waivers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unwaive(@PathVariable Long id) {
        DueWaiver waiver = waivers.findById(id).orElseThrow(() -> ApiException.notFound("Waiver " + id + " not found"));
        Long studentId = sessions.findById(waiver.getSessionId()).map(s -> s.getStudent().getAdmissionNo()).orElse(null);
        waivers.delete(waiver);
        if (studentId != null) {
            events.publishEvent(new StudentDuesInvalidated(studentId));
        }
    }

    @GetMapping("/waivers")
    @Transactional(readOnly = true)
    public List<WaiverResponse> waivers(@RequestParam Long studentId) {
        List<Long> sessionIds = sessions.findAllSessionsByStudentId(studentId).stream().map(StudentSession::getId).toList();
        return waivers.findBySessionIdIn(sessionIds).stream()
                .sorted(Comparator.comparing(DueWaiver::getPeriodStart))
                .map(WaiverResponse::of)
                .toList();
    }

    // ---------- rebuilds (REQ-074) ----------

    @PostMapping("/rebuild")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobStarted rebuild(@RequestParam DuesRebuildJob.Scope scope, @RequestParam(required = false) String id,
                              @RequestParam(defaultValue = "false") boolean dryRun, @AuthenticationPrincipal User user) {
        if (scope != DuesRebuildJob.Scope.ALL && (id == null || id.isBlank())) {
            throw ApiException.invalidField("id", "id is required for scope " + scope);
        }
        if (scope == DuesRebuildJob.Scope.STUDENT) {
            long studentId;
            try {
                studentId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                throw ApiException.invalidField("id", "Student id must be a number");
            }
            if (!students.existsById(studentId)) {
                throw ApiException.notFound("Student " + studentId + " not found");
            }
        }
        if (scope == DuesRebuildJob.Scope.COURSE && !courses.existsById(id)) {
            throw ApiException.notFound("Course '" + id + "' not found");
        }
        return new JobStarted(rebuilds.startJob(scope, scope == DuesRebuildJob.Scope.ALL ? null : id, dryRun,
                user.getEmail()).getId());
    }

    @GetMapping("/rebuild/{jobId}")
    @Transactional(readOnly = true)
    public JobResponse job(@PathVariable Long jobId) {
        DuesRebuildJob job = jobs.findById(jobId).orElseThrow(() -> ApiException.notFound("Job " + jobId + " not found"));
        return new JobResponse(job.getId(), job.getScope(), job.getScopeId(), job.getStatus(), job.isDryRun(),
                job.getCreated(), job.getUpdated(), job.getDeleted(), list(job.getErrors()), list(job.getChanges()),
                job.getStartedAt(), job.getFinishedAt());
    }

    @GetMapping("/rebuild/schedule")
    public Schedule schedule() {
        return new Schedule(nightlyCron, CronExpression.parse(nightlyCron).next(LocalDateTime.now()));
    }

    private List<Object> list(String jsonText) {
        try {
            return json.readValue(jsonText, new TypeReference<>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    // ---------- legacy comparison (REQ-081) ----------

    @GetMapping("/legacy-comparison")
    @Transactional(readOnly = true)
    public Comparison legacyComparison() {
        Boolean present = jdbc.queryForObject("SELECT to_regclass('legacy_fee_dues_tracker') IS NOT NULL", Boolean.class);
        if (!Boolean.TRUE.equals(present)) {
            throw ApiException.notFound("No legacy dues to compare with");
        }
        Map<Long, BigDecimal[]> legacy = new TreeMap<>();
        jdbc.query("""
                SELECT ss.student_id,
                       coalesce(sum(f.amount) FILTER (WHERE d.status <> 'CANCELLED'), 0),
                       coalesce(sum(d.due_amount) FILTER (WHERE d.status IN ('DUE', 'PARTIALLY_PAID')), 0)
                FROM legacy_fee_dues_tracker d
                JOIN legacy_fee_structure f ON f.id = d.fee_structure_id
                JOIN student_sessions ss ON ss.id = d.student_session_id
                GROUP BY ss.student_id""",
                rs -> {
                    legacy.put(rs.getLong(1), new BigDecimal[]{rs.getBigDecimal(2), rs.getBigDecimal(3)});
                });
        Map<Long, BigDecimal[]> current = new TreeMap<>();
        jdbc.query("""
                SELECT student_id, coalesce(sum(amount), 0),
                       coalesce(sum(balance) FILTER (WHERE status <> 'WAIVED'), 0)
                FROM fee_due GROUP BY student_id""",
                rs -> {
                    current.put(rs.getLong(1), new BigDecimal[]{rs.getBigDecimal(2), rs.getBigDecimal(3)});
                });
        BigDecimal[] zero = {BigDecimal.ZERO, BigDecimal.ZERO};
        List<StudentComparison> rows = new ArrayList<>();
        TreeSet<Long> ids = new TreeSet<>(legacy.keySet());
        ids.addAll(current.keySet());
        for (Long id : ids) {
            BigDecimal[] l = legacy.getOrDefault(id, zero);
            BigDecimal[] c = current.getOrDefault(id, zero);
            rows.add(new StudentComparison(id, l[0], c[0], l[1], c[1],
                    l[0].compareTo(c[0]) == 0 && l[1].compareTo(c[1]) == 0));
        }
        Totals totals = new Totals(
                rows.stream().map(StudentComparison::legacyBilled).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(StudentComparison::newBilled).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(StudentComparison::legacyOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(StudentComparison::newOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new Comparison(rows, totals, rows.stream().filter(r -> !r.match()).count());
    }
}
