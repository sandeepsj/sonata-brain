package com.example.sonata_brain.service;

import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.common.context.RequestContext;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dto.PaymentCreateRequest;
import com.example.sonata_brain.dues.DuesService;
import com.example.sonata_brain.logging.ApplicationLoggerFactory;
import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.repository.DiscountRepository;
import com.example.sonata_brain.repository.PaymentRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentSessionRepository studentSessionRepository;

    @Autowired
    private DuesService duesService;

    // Using generic ApplicationLoggerFactory instead of payment-specific logger
    private final ApplicationLoggerFactory logger = ApplicationLoggerFactory.getInstance();

    public Payment createPayment(PaymentCreateRequest request) {
        String paymentType = String.valueOf(request.getPaymentType());

        logger.logOperationStart("PAYMENT_CREATION", "Student:" + request.getStudentId(), "Type: " + paymentType);

        try {
            validate(request);
            Payment payment = new Payment();
            payment.setStudent(studentRepository.getReferenceById(request.getStudentId()));
            payment.setDate(request.getDate() != null ? request.getDate() : java.time.LocalDateTime.now());
            payment.setRemarks(request.getRemarks());
            payment.setLabel(request.getLabel().trim());
            payment.setAmountPaid(request.getAmountPaid());
            payment.setPaymentType(request.getPaymentType());
            payment.setCourseName(blankToNull(request.getCourseName()));
            payment.setDiscountAmount(BigDecimal.ZERO);

            // Save the payment first
            payment = paymentRepository.save(payment);
            logger.logDatabaseOperation("SAVE_SUCCESS", "Payment", "Payment saved with ID: " + payment.getId());

            // Process discounts if any
            if (request.getDiscounts() != null && !request.getDiscounts().isEmpty()) {
                BigDecimal totalDiscountAmount = BigDecimal.ZERO;

                logger.logDatabaseOperation("SAVE", "Discounts", "Processing " + request.getDiscounts().size() + " discounts");

                for (PaymentCreateRequest.DiscountCreateRequest discountRequest : request.getDiscounts()) {
                    Discount discount = new Discount();
                    discount.setDiscountAmount(discountRequest.getDiscountAmount());
                    discount.setLabel(discountRequest.getLabel());
                    discount.setRemarks(discountRequest.getRemarks());
                    discount.setPayment(payment);

                    discountRepository.save(discount);
                    totalDiscountAmount = totalDiscountAmount.add(discountRequest.getDiscountAmount());
                }

                logger.logInfo("DISCOUNT_CREATION",
                        String.format("Created %d discounts for payment ID: %d, total amount: %s",
                                request.getDiscounts().size(), payment.getId(), totalDiscountAmount));

                // Update payment with total discount amount
                payment.setDiscountAmount(totalDiscountAmount);
                payment = paymentRepository.save(payment);
                logger.logDatabaseOperation("UPDATE", "Payment", "Updated payment with discount amount: " + totalDiscountAmount);
            }

            // Dues are recalculated before the response returns (REQ-074.AC1)
            if (payment.getPaymentType() == Payment.PaymentType.REGULAR) {
                paymentRepository.flush();
                duesService.recalculateStudent(request.getStudentId());
            }

            logger.logOperationSuccess("PAYMENT_CREATION", "Student:" + request.getStudentId(), "Payment ID: " + payment.getId());
            return payment;

        } catch (Exception e) {
            logger.logOperationFailure("PAYMENT_CREATION", "Student:" + request.getStudentId(), e.getMessage());
            throw e; // Re-throw to maintain existing error handling
        }
    }

    public List<Payment> getPaymentsByStudentId(Long studentId) {
        return paymentRepository.findByStudentAdmissionNo(studentId);
    }

    public List<Payment> getPaymentsByStudentIdAndType(Long studentId, Payment.PaymentType paymentType) {
        return paymentRepository.findByStudentAdmissionNoAndPaymentType(studentId, paymentType);
    }

    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * REQ-066.AC5: any combination of filters, newest first. {@code from} and {@code to} are inclusive dates. Filtered
     * in the database, with each payment's student and discounts loaded in the same query (not one query per row).
     */
    @Transactional(readOnly = true)
    public List<Payment> findPayments(Long studentId, String courseName, Payment.PaymentType type, Boolean active,
                                      LocalDate from, LocalDate to) {
        Specification<Payment> spec = (root, query, cb) -> {
            root.fetch("student", JoinType.INNER);
            root.fetch("discounts", JoinType.LEFT);
            query.distinct(true);
            List<Predicate> where = new ArrayList<>();
            if (studentId != null) {
                where.add(cb.equal(root.get("student").get("admissionNo"), studentId));
            }
            if (courseName != null) {
                where.add(cb.equal(root.get("courseName"), courseName));
            }
            if (type != null) {
                where.add(cb.equal(root.get("paymentType"), type));
            }
            if (active != null) {
                where.add(cb.equal(root.get("isActive"), active));
            }
            if (from != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("date"), from.atStartOfDay()));
            }
            if (to != null) {
                where.add(cb.lessThan(root.get("date"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(where.toArray(new Predicate[0]));
        };
        return paymentRepository.findAll(spec, Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id")));
    }

    /**
     * Deactivate a payment - the only update operation allowed on payments
     * Once a payment is deactivated, it cannot be reactivated
     */
    public Payment deactivatePayment(Long paymentId) {
        logger.logDatabaseOperation("FIND", "Payment", "Finding payment with ID: " + paymentId);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            logger.logOperationFailure("PAYMENT_DEACTIVATION", "Payment:" + paymentId, "Payment not found");
            throw new IllegalArgumentException("Payment not found with ID: " + paymentId);
        }

        Payment payment = paymentOpt.get();

        // Check if payment is already deactivated
        if (!payment.getIsActive()) {
            logger.logOperationFailure("PAYMENT_DEACTIVATION", "Payment:" + paymentId, "Payment already deactivated");
            throw new IllegalStateException("Payment is already deactivated and cannot be modified");
        }

        // Deactivate the payment
        payment.setIsActive(false);
        Payment savedPayment = paymentRepository.save(payment);

        // Dues are recalculated before the response returns (REQ-064.AC5, REQ-074.AC1)
        if (savedPayment.getPaymentType() == Payment.PaymentType.REGULAR) {
            paymentRepository.flush();
            duesService.recalculateStudent(savedPayment.getStudent().getAdmissionNo());
        }

        logger.logOperationSuccess("PAYMENT_DEACTIVATION", "Payment:" + paymentId, "Payment deactivated successfully");
        return savedPayment;
    }

    // Additional methods for filtering by active status
    public List<Payment> getActivePayments() {
        return paymentRepository.findByIsActive(true);
    }

    public List<Payment> getInactivePayments() {
        return paymentRepository.findByIsActive(false);
    }

    public List<Payment> getPaymentsByStudentIdAndActiveStatus(Long studentId, Boolean isActive) {
        return paymentRepository.findByStudentAdmissionNoAndIsActive(studentId, isActive);
    }

    public List<Payment> getActivePaymentsByStudentId(Long studentId) {
        return paymentRepository.findByStudentAdmissionNoAndIsActive(studentId, true);
    }

    public List<Payment> getPaymentsByStudentIdAndTypeAndActiveStatus(Long studentId, Payment.PaymentType paymentType, Boolean isActive) {
        return paymentRepository.findByStudentAdmissionNoAndPaymentTypeAndIsActive(studentId, paymentType, isActive);
    }

    // Get payments by date range
    public List<Payment> getPaymentsByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return paymentRepository.findByDateBetween(startDate, endDate);
    }

    public List<Payment> getPaymentsByStudentAndDateRange(Long studentId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return paymentRepository.findByStudentAdmissionNoAndDateBetween(studentId, startDate, endDate);
    }

    // Get payments by date range with filters
    public List<Payment> getPaymentsByDateRangeAndCourse(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, String courseName) {
        return paymentRepository.findByDateBetweenAndCourseName(startDate, endDate, courseName);
    }

    public List<Payment> getPaymentsByDateRangeAndType(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Payment.PaymentType paymentType) {
        return paymentRepository.findByDateBetweenAndPaymentType(startDate, endDate, paymentType);
    }

    /**
     * REQ-065: converts an active ADDITIONAL payment into a course fee and brings the student's dues in sync before
     * returning. The id, amount, date, label, remarks and discounts are unchanged.
     */
    public Payment convertToCourseFee(Long paymentId, String requestedCourse, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Payment " + paymentId + " not found"));
        if (payment.getPaymentType() == Payment.PaymentType.REGULAR) {
            throw ApiException.conflict("Payment " + paymentId + " is already a course fee");
        }
        if (!Boolean.TRUE.equals(payment.getIsActive())) {
            throw ApiException.conflict("Payment " + paymentId + " is deactivated and can't be converted");
        }
        String course = blankToNull(requestedCourse) != null ? blankToNull(requestedCourse) : payment.getCourseName();
        if (course == null) {
            throw ApiException.invalidField("courseName", "courseName is required: the payment has no course");
        }
        if (!courseRepository.existsById(course)) {
            throw ApiException.invalidField("courseName", "Course not found: " + course);
        }
        if (courseRepository.findById(course).map(c -> c.isClosed()).orElse(false)) {
            throw ApiException.conflict("Course " + course + " is closed and takes no new course fees"); // REQ-042.AC6
        }
        Long student = payment.getStudent().getAdmissionNo();
        if (studentSessionRepository.findAllSessionsByStudentIdAndCourseName(student, course).isEmpty()) {
            throw ApiException.invalidField("courseName", "Student " + student + " has no session in " + course
                    + ", so there is no course fee to pay");
        }
        if (amount != null && (amount.signum() <= 0 || amount.compareTo(payment.getAmountPaid()) > 0)) {
            throw ApiException.invalidField("amount", "amount must be more than 0 and at most the payment's "
                    + payment.getAmountPaid());
        }
        String by = RequestContext.current().map(RequestContext.Values::user).orElse(null);
        if (amount != null && amount.compareTo(payment.getAmountPaid()) < 0) {
            // REQ-065.AC10: convert only part; the rest stays an additional payment
            Payment part = payment.splitAsCourseFee(amount, course, by);
            paymentRepository.save(payment);
            payment = paymentRepository.saveAndFlush(part);
        } else {
            payment.convertToCourseFee(course, by);
            paymentRepository.saveAndFlush(payment);
        }
        duesService.recalculateStudent(student);
        logger.logInfo("PAYMENT_CONVERSION", "Payment " + paymentId + " converted to a course fee for " + course);
        return payment;
    }

    /** REQ-060.AC14, REQ-061.AC6, REQ-062. Messages are part of the contract (tests match on them). */
    private void validate(PaymentCreateRequest request) {
        if (!request.isValidZeroPayment()) {
            throw new IllegalArgumentException("Zero amount payments are only allowed when discounts are provided");
        }
        if (!studentRepository.existsById(request.getStudentId())) {
            throw new IllegalArgumentException("Student not found with ID: " + request.getStudentId());
        }
        String course = blankToNull(request.getCourseName());
        if (request.isRegularPayment() && course == null) {
            throw new IllegalArgumentException("Course name is required for a REGULAR payment");
        }
        if (course != null && !courseRepository.existsById(course)) {
            throw new IllegalArgumentException("Course not found: " + course);
        }
        if (request.isRegularPayment() && courseRepository.findById(course).map(c -> c.isClosed()).orElse(false)) {
            throw new IllegalStateException("Course " + course + " is closed and takes no new course fees"); // REQ-060.AC9
        }
        if (request.isRegularPayment()
                && studentSessionRepository.findAllSessionsByStudentIdAndCourseName(request.getStudentId(), course).isEmpty()) {
            throw new IllegalArgumentException("Student " + request.getStudentId() + " has no session in " + course
                    + ", so there is no course fee to pay");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // Delete and update methods removed - payments are immutable once created except for deactivation
}
