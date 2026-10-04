package com.example.sonata_brain.dues;

import com.example.sonata_brain.dues.DuesEvents.CourseDuesInvalidated;
import com.example.sonata_brain.dues.DuesEvents.StudentDuesInvalidated;
import com.example.sonata_brain.dues.DuesProjector.PaymentFact;
import com.example.sonata_brain.dues.DuesProjector.ProjectedDue;
import com.example.sonata_brain.dues.DuesProjector.SessionFact;
import com.example.sonata_brain.dues.DuesProjector.WaiverKey;
import com.example.sonata_brain.fee.BillingPeriod;
import com.example.sonata_brain.fee.BillingPeriodCalculator;
import com.example.sonata_brain.fee.FeeTimelineService;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.PaymentRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Keeps the fee_due projection in line with the facts (REQ-070, REQ-074). Every rebuild runs on the single
 * "duesExecutor" thread, one student per transaction, and writes only rows that actually change.
 */
@Service
public class DuesRebuildService {

    private static final Logger log = LoggerFactory.getLogger(DuesRebuildService.class);

    /** A change made (or, in a dry run, that would be made) to one due. */
    public record Change(String action, Long studentId, Long sessionId, LocalDate periodStart) {
    }

    public record Failure(Long studentId, String message) {
    }

    /** Running totals for one rebuild. */
    static final class Tally {
        int created;
        int updated;
        int deleted;
        final List<Change> changes = new ArrayList<>();
        final List<Failure> errors = new ArrayList<>();
    }

    private final StudentSessionRepository sessions;
    private final PaymentRepository payments;
    private final DueWaiverRepository waivers;
    private final FeeDueRepository dues;
    private final DuesRebuildJobRepository jobs;
    private final FeeTimelineService timeline;
    private final TransactionTemplate perStudent;
    private final Executor executor;
    private final ObjectMapper json;

    public DuesRebuildService(StudentSessionRepository sessions, PaymentRepository payments,
                              DueWaiverRepository waivers, FeeDueRepository dues, DuesRebuildJobRepository jobs,
                              FeeTimelineService timeline, PlatformTransactionManager tx,
                              @Qualifier("duesExecutor") Executor executor, ObjectMapper json) {
        this.sessions = sessions;
        this.payments = payments;
        this.waivers = waivers;
        this.dues = dues;
        this.jobs = jobs;
        this.timeline = timeline;
        this.perStudent = new TransactionTemplate(tx);
        this.perStudent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.executor = executor;
        this.json = json;
    }

    // ---------- triggers (REQ-074.AC1, AC2, AC6) ----------

    @Async("duesExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    public void on(StudentDuesInvalidated event) {
        run(List.of(event.studentId()), false, new Tally());
    }

    @Async("duesExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    public void on(CourseDuesInvalidated event) {
        run(studentsOfCourse(event.courseName()), false, new Tally());
    }

    @Scheduled(cron = "${sonata.dues.nightly-cron:0 0 2 * * *}")
    public void nightly() {
        DuesRebuildJob job = jobs.save(new DuesRebuildJob(DuesRebuildJob.Scope.ALL, null, false, "nightly"));
        executor.execute(() -> runJob(job.getId(), allStudents(), false));
    }

    // ---------- jobs (REQ-074.AC3, AC4, AC5) ----------

    /** Records the job and runs it in the background. Callers validate the scope first. */
    public DuesRebuildJob startJob(DuesRebuildJob.Scope scope, String scopeId, boolean dryRun, String user) {
        DuesRebuildJob job = jobs.save(new DuesRebuildJob(scope, scopeId, dryRun, user));
        executor.execute(() -> runJob(job.getId(), studentsFor(scope, scopeId), dryRun));
        return job;
    }

    private void runJob(Long jobId, List<Long> studentIds, boolean dryRun) {
        Tally tally = new Tally();
        DuesRebuildJob.Status status = DuesRebuildJob.Status.DONE;
        try {
            run(studentIds, dryRun, tally);
        } catch (RuntimeException e) {
            log.error("Dues rebuild job {} failed", jobId, e);
            status = DuesRebuildJob.Status.FAILED;
        }
        DuesRebuildJob.Status finalStatus = status;
        perStudent.executeWithoutResult(tx -> jobs.findById(jobId).ifPresent(job -> job.finish(finalStatus,
                tally.created, tally.updated, tally.deleted, toJson(tally.errors), toJson(tally.changes))));
    }

    private List<Long> studentsFor(DuesRebuildJob.Scope scope, String scopeId) {
        return switch (scope) {
            case ALL -> allStudents();
            case COURSE -> studentsOfCourse(scopeId);
            case STUDENT -> List.of(Long.valueOf(scopeId));
        };
    }

    private List<Long> allStudents() {
        Set<Long> ids = new TreeSet<>(sessions.findAllStudentIds());
        ids.addAll(dues.findStudentIds());
        return new ArrayList<>(ids);
    }

    private List<Long> studentsOfCourse(String courseName) {
        Set<Long> ids = new TreeSet<>(sessions.findStudentIdsByCourseName(courseName));
        dues.findAll().stream().filter(d -> d.getCourseName().equals(courseName))
                .forEach(d -> ids.add(d.getStudentId()));
        return new ArrayList<>(ids);
    }

    /** Rebuilds each student in its own transaction; one student's failure doesn't stop the others. */
    private void run(List<Long> studentIds, boolean dryRun, Tally tally) {
        Map<String, List<BillingPeriod>> periodCache = new HashMap<>();
        for (Long studentId : studentIds) {
            try {
                perStudent.executeWithoutResult(tx -> {
                    rebuildStudent(studentId, dryRun, tally, periodCache);
                    if (dryRun) {
                        tx.setRollbackOnly();
                    }
                });
            } catch (RuntimeException e) {
                log.warn("Dues rebuild failed for student {}: {}", studentId, e.getMessage());
                tally.errors.add(new Failure(studentId, e.getMessage() == null ? e.toString() : e.getMessage()));
            }
        }
    }

    // ---------- one student ----------

    private void rebuildStudent(Long studentId, boolean dryRun, Tally tally,
                                Map<String, List<BillingPeriod>> periodCache) {
        List<ProjectedDue> desired = project(studentId, periodCache);
        Map<String, FeeDue> existing = new LinkedHashMap<>();
        dues.findByStudentId(studentId).forEach(d -> existing.put(key(d.getSessionId(), d.getPeriodStart()), d));

        for (ProjectedDue p : desired) {
            FeeDue current = existing.remove(key(p.sessionId(), p.period().periodStart()));
            if (current == null) {
                tally.created++;
                tally.changes.add(new Change("CREATE", studentId, p.sessionId(), p.period().periodStart()));
                if (!dryRun) {
                    dues.save(FeeDue.from(p));
                }
            } else if (!current.sameAs(p)) {
                tally.updated++;
                tally.changes.add(new Change("UPDATE", studentId, p.sessionId(), p.period().periodStart()));
                if (!dryRun) {
                    current.copyFrom(p);
                }
            }
        }
        for (FeeDue stale : existing.values()) {
            tally.deleted++;
            tally.changes.add(new Change("DELETE", studentId, stale.getSessionId(), stale.getPeriodStart()));
            if (!dryRun) {
                dues.delete(stale);
            }
        }
    }

    /** The dues a student should have right now, computed from the facts (used by payment checks too). */
    public List<ProjectedDue> project(Long studentId) {
        return project(studentId, new HashMap<>());
    }

    private List<ProjectedDue> project(Long studentId, Map<String, List<BillingPeriod>> periodCache) {
        List<StudentSession> studentSessions = sessions.findAllSessionsByStudentId(studentId);
        List<SessionFact> sessionFacts = studentSessions.stream()
                .map(s -> new SessionFact(s.getId(), studentId, s.getCourse().getName(),
                        s.getDateOfJoining().toLocalDate(),
                        s.getDateOfDisjoining() == null ? null : s.getDateOfDisjoining().toLocalDate()))
                .toList();
        Map<String, List<BillingPeriod>> periods = new HashMap<>();
        for (SessionFact s : sessionFacts) {
            periods.computeIfAbsent(s.courseName(), c -> periodCache.computeIfAbsent(c, this::periodsOf));
        }
        List<PaymentFact> paymentFacts = payments
                .findByStudentAdmissionNoAndPaymentTypeAndIsActive(studentId, Payment.PaymentType.REGULAR, true)
                .stream()
                .map(p -> new PaymentFact(p.getId(), p.getCourseName(), p.getPeriodDate(), p.getDate(),
                        p.getAmountPaid(), p.getDiscountAmount()))
                .toList();
        Set<WaiverKey> waiverKeys = waivers
                .findBySessionIdIn(sessionFacts.stream().map(SessionFact::sessionId).toList())
                .stream()
                .map(w -> new WaiverKey(w.getSessionId(), w.getPeriodStart()))
                .collect(Collectors.toSet());
        return DuesProjector.project(sessionFacts, periods, paymentFacts, waiverKeys);
    }

    /** Billing periods of a course up to the horizon: the end of the current year (REQ-075). */
    public List<BillingPeriod> periodsOf(String courseName) {
        LocalDate horizon = LocalDate.of(LocalDate.now().getYear(), 12, 31);
        return BillingPeriodCalculator.periods(timeline.slots(courseName), horizon);
    }

    private static String key(Long sessionId, LocalDate periodStart) {
        return sessionId + "|" + periodStart;
    }

    private String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
