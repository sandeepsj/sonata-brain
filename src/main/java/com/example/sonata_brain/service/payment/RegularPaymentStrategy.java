package com.example.sonata_brain.service.payment;

import com.example.sonata_brain.dto.PaymentCreateRequest;
import com.example.sonata_brain.logging.ApplicationLoggerFactory;
import com.example.sonata_brain.model.*;
import com.example.sonata_brain.repository.*;
import com.example.sonata_brain.Repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class RegularPaymentStrategy implements PaymentStrategy {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private StudentSessionRepository studentSessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Using generic ApplicationLoggerFactory instead of payment-specific logger
    private final ApplicationLoggerFactory logger = ApplicationLoggerFactory.getInstance();

    @Override
    public void validatePaymentRequest(PaymentCreateRequest request) {
        logger.logValidationStart("Student:" + request.getStudentId(), "REGULAR_PAYMENT");

        try {
            // Validate zero amount payments are only allowed with discounts
            if (!request.isValidZeroPayment()) {
                String error = "Zero amount payments are only allowed when discounts are provided";
                logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("VALIDATION_SUCCESS", "AmountCheck", "Amount validation passed");

            // Validate student exists
            logger.logDatabaseOperation("CHECK", "Student", "Checking if student exists with ID: " + request.getStudentId());
            if (!studentRepository.existsById(request.getStudentId())) {
                String error = "Student not found with ID: " + request.getStudentId();
                logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("CHECK_SUCCESS", "Student", "Student exists with ID: " + request.getStudentId());

            // Validate term ID is present
            if (request.getTermId() == null) {
                String error = "Term ID is required for regular payments";
                logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("VALIDATION", "TermId", "Term ID provided: " + request.getTermId());

            // Validate course name is present
            if (request.getCourseName() == null || request.getCourseName().trim().isEmpty()) {
                String error = "Course name is required for regular payments";
                logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("VALIDATION", "CourseName", "Course name provided: " + request.getCourseName());

            // Validate course exists
            logger.logDatabaseOperation("CHECK", "Course", "Checking if course exists: " + request.getCourseName());
            if (!courseRepository.existsById(request.getCourseName())) {
                String error = "Course not found: " + request.getCourseName();
                logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("CHECK_SUCCESS", "Course", "Course exists: " + request.getCourseName());

            // Validate fee structure exists
            logger.logDatabaseOperation("CHECK", "FeeStructure", "Checking if fee structure exists with ID: " + request.getTermId());
            Optional<FeeStructure> feeStructure = feeStructureRepository.findById(request.getTermId());
            if (feeStructure.isEmpty()) {
                String error = "Fee structure not found with ID: " + request.getTermId();
                logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("CHECK_SUCCESS", "FeeStructure",
                String.format("Fee structure found - ID: %d, Course: %s, Year: %d, Term: %d, Amount: %s",
                    feeStructure.get().getId(),
                    feeStructure.get().getCourse().getName(),
                    feeStructure.get().getYear(),
                    feeStructure.get().getTerm(),
                    feeStructure.get().getAmount()));

            // Validate that the course name matches the fee structure's course
            String feeStructureCourseName = feeStructure.get().getCourse().getName();
            if (!feeStructureCourseName.equals(request.getCourseName())) {
                String error = String.format("Course name mismatch: Fee structure ID %d is for course '%s' but request specified course '%s'",
                    request.getTermId(), feeStructureCourseName, request.getCourseName());
                logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", error);
                throw new IllegalArgumentException(error);
            }
            logger.logDatabaseOperation("VALIDATION_SUCCESS", "CourseName",
                String.format("Course name matches fee structure: %s", feeStructureCourseName));

            // Validate total payment amount (amountPaid + discounts) doesn't exceed fee structure amount
            validatePaymentAmountLimit(request.getStudentId(), request.getTermId(), request.getAmountPaid(), request.getDiscounts(), feeStructure.get());

            // Validate student has active session for the term
            logger.logDatabaseOperation("VALIDATION", "StudentSession", "Starting session validation for student enrollment");
            validateActiveSessionForTerm(request.getStudentId(), request.getCourseName(), feeStructure.get());
            logger.logDatabaseOperation("VALIDATION_SUCCESS", "StudentSession", "Session validation passed");

            logger.logValidationSuccess("Student:" + request.getStudentId(), "REGULAR_PAYMENT");

        } catch (Exception e) {
            logger.logValidationFailure("Student:" + request.getStudentId(), "REGULAR_PAYMENT", e.getMessage());
            throw e; // Re-throw to maintain existing error handling
        }
    }

    @Override
    public Payment processPayment(PaymentCreateRequest request) {
        validatePaymentRequest(request);

        logger.logDatabaseOperation("CREATE", "Payment", "Creating regular payment entity");
        Payment payment = new Payment();
        payment.setStudent(studentRepository.findById(request.getStudentId()).get());
        payment.setDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
        payment.setRemarks(request.getRemarks());
        payment.setLabel(request.getLabel());
        payment.setAmountPaid(request.getAmountPaid());
        payment.setFeeStructure(feeStructureRepository.findById(request.getTermId()).get());
        payment.setPaymentType(Payment.PaymentType.REGULAR);
        payment.setCourseName(request.getCourseName());

        logger.logDatabaseOperation("CREATE_SUCCESS", "Payment", "Regular payment entity created successfully");
        return payment;
    }

    private void validateActiveSessionForTerm(Long studentId, String courseName, FeeStructure feeStructure) {
        logger.logDatabaseOperation("TERM_CONVERSION", "Term",
            String.format("Converting term to date - Year: %d, Term: %d", feeStructure.getYear(), feeStructure.getTerm()));

        // Dynamically determine the number of terms per year for this course and year
        List<FeeStructure> feeStructuresForYear = feeStructureRepository.findByCourseAndYear(
            feeStructure.getCourse(), feeStructure.getYear());

        int termsInYear = feeStructuresForYear.stream()
            .mapToInt(FeeStructure::getTerm)
            .max()
            .orElse(4); // Default to 4 if no terms found

        logger.logDatabaseOperation("TERM_CALCULATION", "FeeStructure",
            String.format("Found %d fee structures for course %s in year %d. Terms per year: %d",
                feeStructuresForYear.size(), courseName, feeStructure.getYear(), termsInYear));

        // Convert term to date range with actual terms per year
        Term term = new Term(feeStructure.getYear(), feeStructure.getTerm(), termsInYear);
        LocalDateTime termStartDate = term.convertToDate();
        LocalDateTime termEndDate = term.getTermEndDate();

        logger.logDatabaseOperation("TERM_CONVERSION_SUCCESS", "Term",
            String.format("Term period - Start: %s, End: %s", termStartDate, termEndDate));

        // Find active sessions for the student and course
        logger.logDatabaseOperation("QUERY", "StudentSession",
            String.format("Searching for sessions - StudentId: %d, Course: %s", studentId, courseName));

        List<StudentSession> sessions = studentSessionRepository.findByStudentIdAndCourse_Name(studentId, courseName);

        logger.logDatabaseOperation("QUERY_RESULT", "StudentSession",
            String.format("Found %d sessions for student %d in course %s", sessions.size(), studentId, courseName));

        if (sessions.isEmpty()) {
            String error = String.format("No sessions found for student %d in course %s", studentId, courseName);
            logger.logValidationFailure("Student:" + studentId, "REGULAR_PAYMENT", error);
            throw new IllegalArgumentException(error);
        }

        boolean hasValidSession = sessions.stream()
                .anyMatch(session -> {
                    LocalDateTime joiningDate = session.getDateOfJoining();
                    LocalDateTime disjoinDate = session.getDateOfDisjoining();

                    logger.logDatabaseOperation("SESSION_CHECK", "StudentSession",
                        String.format("Checking session ID %d - Joining: %s, Disjoining: %s",
                            session.getId(), joiningDate, disjoinDate));

                    // Check if there's ANY overlap between term period and session period
                    // Student should pay for a term if they were enrolled for ANY part of it
                    // Overlap exists if: session starts before term ends AND session ends after term starts

                    // Session starts before or during the term (joining <= termEnd)
                    boolean sessionStartsBeforeTermEnds = joiningDate.isBefore(termEndDate) || joiningDate.isEqual(termEndDate);

                    // Session ends after or during the term start (disjoining >= termStart OR still enrolled)
                    boolean sessionEndsAfterTermStarts = disjoinDate == null ||
                                                          disjoinDate.isAfter(termStartDate) ||
                                                          disjoinDate.isEqual(termStartDate);

                    // There's overlap if both conditions are true
                    boolean hasOverlap = sessionStartsBeforeTermEnds && sessionEndsAfterTermStarts;

                    logger.logDatabaseOperation("SESSION_CHECK_RESULT", "StudentSession",
                        String.format("Session ID %d - Overlap check: %s (session starts before term ends: %s, session ends after term starts: %s)",
                            session.getId(), hasOverlap, sessionStartsBeforeTermEnds, sessionEndsAfterTermStarts));

                    return hasOverlap;
                });

        if (!hasValidSession) {
            String error = String.format("No active session found for student %d in course %s for term %d/%d. " +
                "Term period: %s to %s",
                studentId, courseName, feeStructure.getYear(), feeStructure.getTerm(),
                termStartDate, termEndDate);
            logger.logValidationFailure("Student:" + studentId, "REGULAR_PAYMENT", error);
            throw new IllegalArgumentException(error);
        }

        logger.logDatabaseOperation("SESSION_VALIDATION_SUCCESS", "StudentSession",
            "Found valid active session for the term period");
    }

    private void validatePaymentAmountLimit(Long studentId, Long termId, BigDecimal newPaymentAmount, List<PaymentCreateRequest.DiscountCreateRequest> newDiscounts, FeeStructure feeStructure) {
        // Calculate total discount amount for new payment
        BigDecimal newDiscountAmount = newDiscounts != null ?
            newDiscounts.stream()
                .map(PaymentCreateRequest.DiscountCreateRequest::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        // Total effective amount for new payment (amountPaid + discounts)
        BigDecimal newTotalAmount = newPaymentAmount.add(newDiscountAmount);

        logger.logDatabaseOperation("AMOUNT_VALIDATION", "Payment",
            String.format("Starting payment amount validation - StudentId: %d, TermId: %d, New Amount: %s, New Discounts: %s, New Total: %s, Fee Structure Amount: %s",
                studentId, termId, newPaymentAmount, newDiscountAmount, newTotalAmount, feeStructure.getAmount()));

        // Find existing active regular payments for this student and term
        List<Payment> existingPayments = paymentRepository.findActiveRegularPaymentsByStudentAndTerm(studentId, termId);

        logger.logDatabaseOperation("AMOUNT_VALIDATION_QUERY", "Payment",
            String.format("Found %d existing active regular payments for student %d and term %d",
                existingPayments.size(), studentId, termId));

        // Calculate total amount already paid for this term (amountPaid + discounts)
        BigDecimal totalExistingEffectiveAmount = existingPayments.stream()
                .map(payment -> payment.getAmountPaid().add(payment.getDiscountAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.logDatabaseOperation("AMOUNT_CALCULATION", "Payment",
            String.format("Total existing effective amount (paid + discounts): %s", totalExistingEffectiveAmount));

        // Calculate what the total would be after adding the new payment
        BigDecimal totalAfterNewPayment = totalExistingEffectiveAmount.add(newTotalAmount);

        logger.logDatabaseOperation("AMOUNT_CALCULATION", "Payment",
            String.format("Total effective amount after new payment: %s (existing: %s + new: %s)",
                totalAfterNewPayment, totalExistingEffectiveAmount, newTotalAmount));

        // Check if total exceeds fee structure amount
        if (totalAfterNewPayment.compareTo(feeStructure.getAmount()) > 0) {
            String error = String.format(
                "Total effective payment amount (%s) would exceed the fee structure amount (%s) for term %d/%d. " +
                "Existing effective amount: %s, New payment amount: %s, New discounts: %s, Total new: %s, Allowed amount: %s",
                totalAfterNewPayment, feeStructure.getAmount(),
                feeStructure.getYear(), feeStructure.getTerm(),
                totalExistingEffectiveAmount, newPaymentAmount, newDiscountAmount, newTotalAmount, feeStructure.getAmount());

            logger.logValidationFailure("Student:" + studentId, "REGULAR_PAYMENT", error);
            throw new IllegalArgumentException(error);
        }

        logger.logDatabaseOperation("AMOUNT_VALIDATION_SUCCESS", "Payment",
            String.format("Payment amount validation passed - Total: %s, Limit: %s, Remaining: %s",
                totalAfterNewPayment, feeStructure.getAmount(),
                feeStructure.getAmount().subtract(totalAfterNewPayment)));
    }
}
