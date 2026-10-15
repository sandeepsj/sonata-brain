package com.example.sonata_brain.dues;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesDtos.Changes;
import com.example.sonata_brain.dues.DuesDtos.CourseSummary;
import com.example.sonata_brain.dues.DuesDtos.DueResponse;
import com.example.sonata_brain.dues.DuesDtos.Summary;
import com.example.sonata_brain.fees.Timeline;
import com.example.sonata_brain.fees.TimelineEntry;
import com.example.sonata_brain.fees.TimelineEntryRepository;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.PaymentRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
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
 * deleted. Unchanged rows keep their ids, so recalculating twice changes nothing.
 */
@Service
@Transactional
public class DuesService {

    private static final Logger log = LoggerFactory.getLogger(DuesService.class);

    private final FeeDueRepository dues;
    private final TimelineEntryRepository timeline;
    private final StudentSessionRepository sessions;
    private final PaymentRepository payments;
    private final StudentRepository students;

    public DuesService(FeeDueRepository dues, TimelineEntryRepository timeline, StudentSessionRepository sessions,
                       PaymentRepository payments, StudentRepository students) {
        this.dues = dues;
        this.timeline = timeline;
        this.sessions = sessions;
        this.payments = payments;
        this.students = students;
    }

    /** Recalculates one student (REQ-074.AC1). */
    public Changes recalculateStudent(Long admissionNo) {
        return recalculate(admissionNo, startedTerms());
    }

    /** Recalculates everyone in the current transaction (REQ-074.AC7). */
    public Changes recalculateAll() {
        List<Timeline.Term> terms = startedTerms();
        Changes total = new Changes(0, 0, 0);
        for (Long id : studentIds()) {
            total = total.plus(recalculate(id, terms));
        }
        log.info("Recalculated dues for everyone: {}", total);
        return total;
    }

    @Transactional(readOnly = true)
    public List<Long> studentIds() {
        return students.findAll(Sort.by("admissionNo")).stream().map(Student::getAdmissionNo).toList();
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

    private Changes recalculate(Long admissionNo, List<Timeline.Term> terms) {
        Student student = students.getReferenceById(admissionNo);
        DuesCalculator.Result result = calculate(admissionNo, terms);
        Map<String, FeeDue> stored = new HashMap<>();
        dues.findByStudent_AdmissionNo(admissionNo).forEach(d -> stored.put(key(d.getCourseName(), d.getPeriodStart()), d));
        int inserted = 0;
        int updated = 0;
        List<FeeDue> toSave = new ArrayList<>();
        for (DuesCalculator.Due due : result.dues()) {
            FeeDue row = stored.remove(key(due.course(), due.term().start()));
            if (row == null) {
                row = new FeeDue(student, due.course(), due.term().start());
                row.apply(due);
                toSave.add(row);
                inserted++;
            } else if (row.apply(due)) {
                toSave.add(row);
                updated++;
            }
        }
        dues.deleteAll(stored.values());
        dues.saveAll(toSave);
        return new Changes(inserted, updated, stored.size());
    }

    private DuesCalculator.Result calculate(Long admissionNo, List<Timeline.Term> terms) {
        List<DuesCalculator.Session> spans = sessions.findAllSessionsByStudentId(admissionNo).stream()
                .filter(s -> s.getCourse() != null)
                .map(DuesService::span)
                .toList();
        List<DuesCalculator.Payment> paid = payments
                .findByStudentAdmissionNoAndPaymentTypeAndIsActive(admissionNo, Payment.PaymentType.REGULAR, true)
                .stream()
                .filter(p -> p.getCourseName() != null)
                .map(p -> new DuesCalculator.Payment(p.getId(), p.getCourseName(), p.getDate(), p.getAmountPaid(),
                        p.getDiscountAmount()))
                .toList();
        return DuesCalculator.calculate(terms, spans, paid);
    }

    private static DuesCalculator.Session span(StudentSession s) {
        return new DuesCalculator.Session(s.getCourse().getName(), s.getDateOfJoining().toLocalDate(),
                s.getDateOfDisjoining() == null ? null : s.getDateOfDisjoining().toLocalDate());
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
