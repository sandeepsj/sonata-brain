package com.example.sonata_brain.service.payment;

import com.example.sonata_brain.dto.PaymentCreateRequest;
import com.example.sonata_brain.logging.ApplicationLoggerFactory;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.Repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdditionalPaymentStrategy implements PaymentStrategy {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    // Using generic ApplicationLoggerFactory instead of payment-specific logger
    private final ApplicationLoggerFactory logger = ApplicationLoggerFactory.getInstance();

    @Override
    public void validatePaymentRequest(PaymentCreateRequest request) {
        logger.logValidationStart("Student:" + request.getStudentId(), "ADDITIONAL_PAYMENT");

        try {
            // Validate zero amount payments are only allowed with discounts
            if (!request.isValidZeroPayment()) {
                String error = "Zero amount payments are only allowed when discounts are provided";
                logger.logValidationFailure("Student:" + request.getStudentId(), "ADDITIONAL_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("VALIDATION_SUCCESS", "AmountCheck", "Amount validation passed");

            // Validate student exists
            logger.logDatabaseOperation("CHECK", "Student", "Checking if student exists with ID: " + request.getStudentId());
            if (!studentRepository.existsById(request.getStudentId())) {
                String error = "Student not found with ID: " + request.getStudentId();
                logger.logValidationFailure("Student:" + request.getStudentId(), "ADDITIONAL_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("CHECK_SUCCESS", "Student", "Student exists with ID: " + request.getStudentId());

            // Term ID should not be present for additional payments
            if (request.getTermId() != null) {
                String error = "Term ID should not be provided for additional payments";
                logger.logValidationFailure("Student:" + request.getStudentId(), "ADDITIONAL_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("VALIDATION_SUCCESS", "TermId", "No term ID provided (correct for additional payment)");

            // Course name is optional for additional payments, but if provided, it should be valid
            if (request.getCourseName() != null && !request.getCourseName().trim().isEmpty()) {
                logger.logDatabaseOperation("CHECK", "Course", "Course name provided, checking if exists: " + request.getCourseName());
                if (!courseRepository.existsById(request.getCourseName())) {
                    String error = "Course not found: " + request.getCourseName();
                    logger.logValidationFailure("Student:" + request.getStudentId(), "ADDITIONAL_PAYMENT", error);
                    throw new IllegalArgumentException(error);
                }
                logger.logDatabaseOperation("CHECK_SUCCESS", "Course", "Course exists: " + request.getCourseName());
            } else {
                logger.logDatabaseOperation("VALIDATION", "Course", "No course name provided (optional for additional payments)");
            }

            logger.logValidationSuccess("Student:" + request.getStudentId(), "ADDITIONAL_PAYMENT");

        } catch (Exception e) {
            logger.logValidationFailure("Student:" + request.getStudentId(), "ADDITIONAL_PAYMENT", e.getMessage());
            throw e; // Re-throw to maintain existing error handling
        }
    }

    @Override
    public Payment processPayment(PaymentCreateRequest request) {
        validatePaymentRequest(request);

        logger.logDatabaseOperation("CREATE", "Payment", "Creating additional payment entity");
        Payment payment = new Payment();
        payment.setStudent(studentRepository.findById(request.getStudentId()).get());
        payment.setDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
        payment.setRemarks(request.getRemarks());
        payment.setLabel(request.getLabel());
        payment.setAmountPaid(request.getAmountPaid());
        payment.setPaymentType(Payment.PaymentType.ADDITIONAL);
        payment.setCourseName(request.getCourseName());

        logger.logDatabaseOperation("CREATE_SUCCESS", "Payment", "Additional payment entity created successfully");
        return payment;
    }
}
