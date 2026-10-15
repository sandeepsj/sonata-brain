package com.example.sonata_brain.service;

import com.example.sonata_brain.dto.PaymentCreateRequest;
import com.example.sonata_brain.logging.ApplicationLoggerFactory;
import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.repository.DiscountRepository;
import com.example.sonata_brain.repository.PaymentRepository;
import com.example.sonata_brain.Repository.StudentRepository;
import com.example.sonata_brain.common.context.RequestContext;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesService;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public Payment convertToCourseFee(Long paymentId, String requestedCourse) {
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
        Long student = payment.getStudent().getAdmissionNo();
        if (studentSessionRepository.findAllSessionsByStudentIdAndCourseName(student, course).isEmpty()) {
            throw ApiException.invalidField("courseName", "Student " + student + " has no session in " + course
                    + ", so there is no course fee to pay");
        }
        String by = RequestContext.current().map(RequestContext.Values::user).orElse(null);
        payment.convertToCourseFee(course, by);
        paymentRepository.saveAndFlush(payment);
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
