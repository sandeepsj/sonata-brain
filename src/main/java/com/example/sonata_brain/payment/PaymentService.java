package com.example.sonata_brain.payment;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.common.error.ApiError.FieldProblem;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.course.CourseService;
import com.example.sonata_brain.dues.DuesEvents.StudentDuesInvalidated;
import com.example.sonata_brain.dues.DuesProjector.ProjectedDue;
import com.example.sonata_brain.dues.DuesRebuildService;
import com.example.sonata_brain.dues.FeeDue;
import com.example.sonata_brain.fee.BillingPeriod;
import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.payment.PaymentDtos.DiscountRequest;
import com.example.sonata_brain.payment.PaymentDtos.PaymentRequest;
import com.example.sonata_brain.payment.PaymentDtos.PaymentResponse;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Payments (REQ-060 to REQ-066). Payments are facts; dues are recomputed from them after every change. */
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository payments;
    private final StudentRepository students;
    private final CourseRepository courseRepository;
    private final CourseService courses;
    private final DuesRebuildService dues;
    private final ApplicationEventPublisher events;

    public PaymentService(PaymentRepository payments, StudentRepository students, CourseRepository courseRepository,
                          CourseService courses, DuesRebuildService dues, ApplicationEventPublisher events) {
        this.payments = payments;
        this.students = students;
        this.courseRepository = courseRepository;
        this.courses = courses;
        this.dues = dues;
        this.events = events;
    }

    public PaymentResponse create(PaymentRequest r) {
        if (r == null) {
            throw ApiException.badRequest("A request body is required");
        }
        validate(r);
        Student student = students.findById(r.studentId())
                .orElseThrow(() -> ApiException.notFound("Student " + r.studentId() + " not found"));
        BigDecimal discountTotal = r.discounts() == null ? BigDecimal.ZERO : r.discounts().stream()
                .map(DiscountRequest::discountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (r.paymentType() == Payment.PaymentType.REGULAR) {
            courses.requireActive(r.courseName());
            checkCourseFee(student.getAdmissionNo(), r.courseName(), r.periodDate(), r.amountPaid().add(discountTotal));
        } else if (r.courseName() != null && !courseRepository.existsById(r.courseName())) {
            throw ApiException.invalidField("courseName", "Course '" + r.courseName() + "' not found");
        }

        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setPaymentType(r.paymentType());
        payment.setCourseName(r.courseName());
        payment.setPeriodDate(r.paymentType() == Payment.PaymentType.REGULAR ? r.periodDate() : null);
        payment.setAmountPaid(r.amountPaid());
        payment.setDiscountAmount(discountTotal);
        payment.setLabel(r.label().trim());
        payment.setRemarks(r.remarks());
        payment.setDate(r.date() != null ? r.date() : LocalDateTime.now());
        if (r.discounts() != null) {
            for (DiscountRequest d : r.discounts()) {
                Discount discount = new Discount(d.discountAmount(), d.label().trim(), d.remarks());
                discount.setPayment(payment);
                payment.getDiscounts().add(discount);
            }
        }
        Payment saved = payments.save(payment);
        invalidate(saved);
        return PaymentResponse.of(saved);
    }

    public PaymentResponse deactivate(Long id) {
        Payment payment = find(id);
        if (!payment.getIsActive()) {
            throw ApiException.conflict("Payment " + id + " is already deactivated");
        }
        payment.setIsActive(false);
        invalidate(payment);
        return PaymentResponse.of(payment);
    }

    /** Re-labels an additional payment as a course fee for a period (REQ-065). */
    public PaymentResponse convertToCourseFee(Long id, String courseName, LocalDate periodDate, String by) {
        Payment payment = find(id);
        if (payment.getPaymentType() != Payment.PaymentType.ADDITIONAL) {
            throw ApiException.conflict("Payment " + id + " is already a course fee");
        }
        if (!payment.getIsActive()) {
            throw ApiException.conflict("Payment " + id + " is deactivated");
        }
        String course = courseName != null ? courseName : payment.getCourseName();
        List<FieldProblem> problems = new ArrayList<>();
        if (course == null) {
            problems.add(new FieldProblem("courseName", "A course is required"));
        }
        if (periodDate == null) {
            problems.add(new FieldProblem("periodDate", "periodDate is required"));
        }
        if (!problems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation failed", problems);
        }
        courses.requireActive(course);
        checkCourseFee(payment.getStudent().getAdmissionNo(), course, periodDate, payment.settledAmount());
        payment.convertToCourseFee(course, periodDate, by);
        invalidate(payment);
        return PaymentResponse.of(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        return PaymentResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> search(Long studentId, String courseName, Payment.PaymentType type, Boolean active,
                                        LocalDate from, LocalDate to, LocalDate periodDate) {
        Stream<Payment> s = (studentId != null ? payments.findByStudentAdmissionNo(studentId) : payments.findAll()).stream();
        if (courseName != null) {
            s = s.filter(p -> courseName.equals(p.getCourseName()));
        }
        if (type != null) {
            s = s.filter(p -> p.getPaymentType() == type);
        }
        if (active != null) {
            s = s.filter(p -> p.getIsActive().equals(active));
        }
        if (from != null) {
            s = s.filter(p -> !p.getDate().toLocalDate().isBefore(from));
        }
        if (to != null) {
            s = s.filter(p -> !p.getDate().toLocalDate().isAfter(to));
        }
        if (periodDate != null) {
            s = s.filter(p -> p.getPaymentType() == Payment.PaymentType.REGULAR && p.getPeriodDate() != null
                    && periodContaining(p.getCourseName(), periodDate)
                            .map(period -> period.contains(p.getPeriodDate())).orElse(false));
        }
        return s.sorted(Comparator.comparing(Payment::getDate).thenComparing(Payment::getId).reversed())
                .map(PaymentResponse::of)
                .toList();
    }

    /**
     * A course fee must name a billed period the student was enrolled in, and must not exceed what is left to pay
     * for it (REQ-060.AC7, AC8).
     */
    private void checkCourseFee(Long studentId, String course, LocalDate periodDate, BigDecimal settled) {
        BillingPeriod period = periodContaining(course, periodDate).orElseThrow(() -> ApiException.badRequest(
                "periodDate " + periodDate + " falls in no billing period of course '" + course + "'"));
        ProjectedDue due = dues.project(studentId).stream()
                .filter(d -> d.courseName().equals(course) && d.period().periodStart().equals(period.periodStart()))
                .min(Comparator.comparing(d -> d.status() == FeeDue.Status.WAIVED))   // prefer a payable due
                .orElseThrow(() -> ApiException.badRequest("Student " + studentId + " is not enrolled in '" + course
                        + "' during the period " + period.periodStart() + " to " + period.periodEnd()));
        if (settled.compareTo(due.balance()) > 0) {
            throw ApiException.badRequest("Payment of " + plain(settled) + " exceeds the remaining balance of "
                    + plain(due.balance()) + " for the period " + period.periodStart() + " to " + period.periodEnd());
        }
    }

    private Optional<BillingPeriod> periodContaining(String course, LocalDate day) {
        return dues.periodsOf(course).stream().filter(p -> p.contains(day)).findFirst();
    }

    private void invalidate(Payment payment) {
        if (payment.getPaymentType() == Payment.PaymentType.REGULAR) {
            events.publishEvent(new StudentDuesInvalidated(payment.getStudent().getAdmissionNo()));
        }
    }

    private Payment find(Long id) {
        return payments.findById(id).orElseThrow(() -> ApiException.notFound("Payment " + id + " not found"));
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /** Collects every field problem so one 400 lists them all. */
    private static void validate(PaymentRequest r) {
        List<FieldProblem> problems = new ArrayList<>();
        if (r.studentId() == null) {
            problems.add(new FieldProblem("studentId", "studentId is required"));
        }
        if (r.paymentType() == null) {
            problems.add(new FieldProblem("paymentType", "paymentType must be REGULAR or ADDITIONAL"));
        }
        if (r.amountPaid() == null) {
            problems.add(new FieldProblem("amountPaid", "amountPaid is required"));
        } else if (r.amountPaid().signum() < 0) {
            problems.add(new FieldProblem("amountPaid", "amountPaid must not be negative"));
        }
        if (r.label() == null || r.label().isBlank()) {
            problems.add(new FieldProblem("label", "A label is required"));
        }
        if (r.discounts() != null) {
            for (int i = 0; i < r.discounts().size(); i++) {
                DiscountRequest d = r.discounts().get(i);
                if (d == null || d.discountAmount() == null || d.discountAmount().signum() <= 0) {
                    problems.add(new FieldProblem("discounts[" + i + "].discountAmount", "Discount must be positive"));
                }
                if (d == null || d.label() == null || d.label().isBlank()) {
                    problems.add(new FieldProblem("discounts[" + i + "].label", "Discount label is required"));
                }
            }
        }
        if (r.paymentType() == Payment.PaymentType.REGULAR) {
            if (r.courseName() == null || r.courseName().isBlank()) {
                problems.add(new FieldProblem("courseName", "A course fee needs a course"));
            }
            if (r.periodDate() == null) {
                problems.add(new FieldProblem("periodDate", "A course fee needs the date of the period it pays for"));
            }
        } else if (r.paymentType() == Payment.PaymentType.ADDITIONAL && r.periodDate() != null) {
            problems.add(new FieldProblem("periodDate", "An additional payment has no period"));
        }
        if (!problems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation failed", problems);
        }
        boolean noDiscounts = r.discounts() == null || r.discounts().isEmpty();
        if (r.amountPaid().signum() == 0 && noDiscounts) {
            throw ApiException.invalidField("amountPaid", "Zero amount payments are only allowed with a discount");
        }
    }
}
