package com.example.sonata_brain.dues;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesDtos.Changes;
import com.example.sonata_brain.dues.DuesDtos.CourseSummary;
import com.example.sonata_brain.dues.DuesDtos.DueResponse;
import com.example.sonata_brain.dues.DuesDtos.Impact;
import com.example.sonata_brain.dues.DuesDtos.Summary;
import com.example.sonata_brain.fees.Timeline;
import com.example.sonata_brain.fees.TimelineEntry;
import com.example.sonata_brain.fees.TimelineEntryRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Keeps {@code fee_due} equal to what {@link DuesCalculator} says (REQ-070, REQ-074). A student's stored dues are
 * compared with the calculated ones by (course, term start): changed rows are updated, new ones inserted, stale ones
 * deleted. Unchanged rows keep their ids, so recalculating twice changes nothing. Reads and writes go through
 * {@link DuesStore} in bulk.
 */
@Service
@Transactional
public class DuesService {

    private static final Logger log = LoggerFactory.getLogger(DuesService.class);

    private final FeeDueRepository dues;
    private final TimelineEntryRepository timeline;
    private final StudentRepository students;
    private final DuesStore store;
    private final EntityManager entities;

    public DuesService(FeeDueRepository dues, TimelineEntryRepository timeline, StudentRepository students,
                       DuesStore store, EntityManager entities) {
        this.dues = dues;
        this.timeline = timeline;
        this.students = students;
        this.store = store;
        this.entities = entities;
    }

    /** Recalculates one student (REQ-074.AC1). */
    public Changes recalculateStudent(Long admissionNo) {
        entities.flush(); // facts saved through JPA earlier in this transaction must be visible to the SQL reads
        return recalculate(List.of(admissionNo), startedTerms(), store.sessions(admissionNo),
                store.payments(admissionNo), store.dues(admissionNo), false).changes();
    }

    /**
     * Recalculates everyone in the current transaction (REQ-074.AC7): every fact is read in one query each and the
     * changes are written in batches, so the cost doesn't grow with round trips per student.
     */
    public Changes recalculateAll() {
        return recalculateAll(false).changes();
    }

    /** Everyone, reporting the impact; with {@code dryRun} nothing is written (REQ-058.AC10, AC11). */
    public Impact recalculateAll(boolean dryRun) {
        entities.flush();
        List<Long> ids = store.studentIds();
        Impact impact = recalculate(ids, startedTerms(), store.sessions(null), store.payments(null), store.dues(null),
                dryRun);
        log.info("{} dues for {} students in {} ms: {}", dryRun ? "Dry run of" : "Recalculated", ids.size(),
                impact.durationMs(), impact);
        return impact;
    }

    @Transactional(readOnly = true)
    public List<Long> studentIds() {
        return store.studentIds();
    }

    @Transactional(readOnly = true)
    public boolean exists(Long admissionNo) {
        return students.existsById(admissionNo);
    }

    /** REQ-076.AC4. */
    @Transactional(readOnly = true)
    public List<DueResponse> list(Long studentId, String courseName, DueStatus status, LocalDate from, LocalDate to) {
        Specification<FeeDue> spec = (root, query, cb) -> cb.conjunction();
        if (studentId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("student").get("admissionNo"), studentId));
        }
        if (courseName != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("courseName"), courseName));
        }
        if (status != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("periodStart"), from));
        }
        if (to != null) {
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("periodStart"), to));
        }
        Sort order = Sort.by("student.admissionNo", "courseName", "periodStart");
        return dues.findAll(spec, order).stream().map(DueResponse::of).toList();
    }

    /** REQ-076.AC7: totals per course and overall, with credit calculated the same way as the dues. */
    @Transactional(readOnly = true)
    public Summary summary(Long studentId) {
        if (!students.existsById(studentId)) {
            throw ApiException.notFound("Student " + studentId + " not found");
        }
        DuesCalculator.Result result = calculate(studentId, startedTerms());
        Map<String, BigDecimal[]> byCourse = new TreeMap<>();
        result.credit().keySet().forEach(c -> byCourse.put(c, zeros()));
        for (DuesCalculator.Due d : result.dues()) {
            BigDecimal[] t = byCourse.computeIfAbsent(d.course(), c -> zeros());
            t[0] = t[0].add(d.amount());
            t[1] = t[1].add(d.paid());
            t[2] = t[2].add(d.discount());
            t[3] = t[3].add(d.balance());
        }
        List<CourseSummary> courses = new ArrayList<>();
        BigDecimal[] total = zeros();
        BigDecimal credit = BigDecimal.ZERO;
        for (var e : byCourse.entrySet()) {
            BigDecimal[] t = e.getValue();
            BigDecimal c = result.credit().getOrDefault(e.getKey(), BigDecimal.ZERO);
            courses.add(new CourseSummary(e.getKey(), t[0], t[1], t[2], t[3], c));
            for (int i = 0; i < 4; i++) {
                total[i] = total[i].add(t[i]);
            }
            credit = credit.add(c);
        }
        return new Summary(studentId, courses, total[0], total[1], total[2], total[3], credit);
    }

    /** Compares the calculated dues of these students with the stored ones and writes only the differences. */
    private Impact recalculate(List<Long> ids, List<Timeline.Term> terms,
                               Map<Long, List<DuesCalculator.Session>> sessions,
                               Map<Long, List<DuesCalculator.Payment>> payments,
                               Map<Long, List<DuesStore.Stored>> dues, boolean dryRun) {
        long started = System.currentTimeMillis();
        BigDecimal before = BigDecimal.ZERO;
        BigDecimal after = BigDecimal.ZERO;
        int affected = 0;
        List<DuesStore.Row> inserts = new ArrayList<>();
        List<DuesStore.Row> updates = new ArrayList<>();
        List<Long> deletes = new ArrayList<>();
        for (Long id : ids) {
            DuesCalculator.Result result = DuesCalculator.calculate(terms, sessions.getOrDefault(id, List.of()),
                    payments.getOrDefault(id, List.of()));
            int changesBefore = inserts.size() + updates.size() + deletes.size();
            for (DuesStore.Stored d : dues.getOrDefault(id, List.of())) {
                before = before.add(d.balance());
            }
            for (DuesCalculator.Due d : result.dues()) {
                after = after.add(d.balance());
            }
            Map<String, DuesStore.Stored> stored = new HashMap<>();
            dues.getOrDefault(id, List.of()).forEach(d -> stored.put(key(d.course(), d.periodStart()), d));
            for (DuesCalculator.Due due : result.dues()) {
                DuesStore.Stored existing = stored.remove(key(due.course(), due.term().start()));
                DuesStore.Row row = new DuesStore.Row(existing == null ? null : existing.id(), id, due.course(),
                        due.term().start(), due.term().end(), due.term().structure().getId(), due.amount(),
                        due.paid(), due.discount(), due.balance(), due.status());
                if (existing == null) {
                    inserts.add(row);
                } else if (differs(existing, row)) {
                    updates.add(row);
                }
            }
            stored.values().forEach(d -> deletes.add(d.id()));
            if (inserts.size() + updates.size() + deletes.size() > changesBefore) {
                affected++;
            }
        }
        if (!dryRun) {
            store.write(inserts, updates, deletes);
        }
        return new Impact(affected, inserts.size(), updates.size(), deletes.size(), before, after,
                after.subtract(before), System.currentTimeMillis() - started);
    }

    private static boolean differs(DuesStore.Stored a, DuesStore.Row b) {
        return !a.periodEnd().equals(b.periodEnd()) || a.structureId() != b.structureId()
                || a.amount().compareTo(b.amount()) != 0 || a.paid().compareTo(b.paid()) != 0
                || a.discount().compareTo(b.discount()) != 0 || a.balance().compareTo(b.balance()) != 0
                || a.status() != b.status();
    }

    private DuesCalculator.Result calculate(Long admissionNo, List<Timeline.Term> terms) {
        return DuesCalculator.calculate(terms, store.sessions(admissionNo).getOrDefault(admissionNo, List.of()),
                store.payments(admissionNo).getOrDefault(admissionNo, List.of()));
    }

    /** Terms that have started (REQ-075.AC2). */
    private List<Timeline.Term> startedTerms() {
        List<Timeline.Entry> entries = timeline.findAllByOrderByEffectiveFromAsc().stream()
                .map(TimelineEntry::toEntry).toList();
        if (entries.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return Timeline.terms(entries, entries.get(0).from(), today);
    }

    private static String key(String course, LocalDate start) {
        return course + "|" + start;
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }
}
