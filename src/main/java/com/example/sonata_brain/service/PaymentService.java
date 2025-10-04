package com.example.sonata_brain.service;

import com.example.sonata_brain.dto.PaymentCreateRequest;
import com.example.sonata_brain.logging.ApplicationLoggerFactory;
import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.repository.DiscountRepository;
import com.example.sonata_brain.repository.PaymentRepository;
import com.example.sonata_brain.service.payment.AdditionalPaymentStrategy;
import com.example.sonata_brain.service.payment.PaymentStrategy;
import com.example.sonata_brain.service.payment.RegularPaymentStrategy;
import com.example.sonata_brain.service.dues.events.DuesTrackerEventPublisher;
import com.example.sonata_brain.service.dues.events.PaymentCreatedEvent;
import com.example.sonata_brain.service.dues.events.PaymentDeactivatedEvent;
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
    private RegularPaymentStrategy regularPaymentStrategy;

    @Autowired
    private AdditionalPaymentStrategy additionalPaymentStrategy;

    @Autowired
    private DuesTrackerEventPublisher eventPublisher;

    // Using generic ApplicationLoggerFactory instead of payment-specific logger
    private final ApplicationLoggerFactory logger = ApplicationLoggerFactory.getInstance();

    public Payment createPayment(PaymentCreateRequest request) {
        String paymentType = request.isRegularPayment() ? "REGULAR" : "ADDITIONAL";

        logger.logOperationStart("PAYMENT_CREATION", "Student:" + request.getStudentId(), "Type: " + paymentType);

        try {
            // Choose strategy based on payment type
            PaymentStrategy strategy = request.isRegularPayment() ? regularPaymentStrategy : additionalPaymentStrategy;

            logger.logStrategySelection(strategy.getClass().getSimpleName(),
                    request.isRegularPayment() ? "termId present" : "termId not present");

            // Process payment using the appropriate strategy
            logger.logDatabaseOperation("SAVE", "Payment", "Saving payment entity");
            Payment payment = strategy.processPayment(request);

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

            // Publish event for dues tracking (only for regular payments)
            if (payment.getPaymentType() == Payment.PaymentType.REGULAR) {
                eventPublisher.publishEvent(new PaymentCreatedEvent(this, payment));
                logger.logInfo("DUES_EVENT_PUBLISHED", "Published dues tracking event for payment ID: " + payment.getId());
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

    public List<Payment> getPaymentsByTermId(Long termId) {
        return paymentRepository.findByTermId(termId);
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

        // Publish event for dues tracking (only for regular payments)
        if (savedPayment.getPaymentType() == Payment.PaymentType.REGULAR) {
            eventPublisher.publishEvent(new PaymentDeactivatedEvent(this, savedPayment));
            logger.logInfo("DUES_EVENT_PUBLISHED", "Published dues tracking event for deactivated payment ID: " + savedPayment.getId());
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

    public List<Payment> getPaymentsByDateRangeAndTerm(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Long termId) {
        return paymentRepository.findByDateBetweenAndTermId(startDate, endDate, termId);
    }

    // Delete and update methods removed - payments are immutable once created except for deactivation
}
