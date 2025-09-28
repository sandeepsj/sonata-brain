package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Scope("prototype")
public class UpdateDuesForPaymentCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDuesForPaymentCommand.class);

    @Autowired
    private FeeDuesTrackerService feeDuesTrackerService;

    @Autowired
    private DuesCalculationService duesCalculationService;

    private final Payment payment;

    public UpdateDuesForPaymentCommand(Payment payment) {
        this.payment = payment;
    }

    @Override
    public void execute() {
        logger.info("Updating dues for payment ID: {} (Student: {}, Amount: {})",
                   payment.getId(), payment.getStudent().getAdmissionNo(), payment.getAmountPaid());

        // Only process regular payments that have fee structure info
        if (payment.getPaymentType() != Payment.PaymentType.REGULAR || payment.getFeeStructure() == null) {
            logger.debug("Skipping non-regular payment or payment without fee structure");
            return;
        }

        // Get the term information from fee structure
        Term term = duesCalculationService.getTermFromFeeStructure(payment.getFeeStructure());
        if (term == null) {
            logger.warn("Could not determine term from fee structure for payment {}", payment.getId());
            return;
        }

        // Find matching dues for this payment
        List<FeeDuesTracker> matchingDues = feeDuesTrackerService.findMatchingDuesForPayment(
            payment.getStudent().getAdmissionNo(),
            payment.getCourseName(),
            term.getYear(),
            term.getTermNumber()
        );

        if (matchingDues.isEmpty()) {
            logger.warn("No matching dues found for payment {}", payment.getId());
            return;
        }

        // Apply payment to dues (FIFO - oldest first)
        BigDecimal remainingPayment = payment.getFinalAmount(); // Amount after discounts
        int updatedCount = 0;

        for (FeeDuesTracker dues : matchingDues) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break; // Payment fully applied
            }

            if (dues.getStatus() == FeeDuesTracker.DuesStatus.PAID) {
                continue; // Skip already paid dues
            }

            BigDecimal paymentToApply = remainingPayment.min(dues.getDueAmount());
            dues.updateDueAmount(paymentToApply, "Payment ID: " + payment.getId());
            feeDuesTrackerService.save(dues);

            remainingPayment = remainingPayment.subtract(paymentToApply);
            updatedCount++;

            logger.debug("Applied {} to dues ID: {}, remaining payment: {}",
                        paymentToApply, dues.getId(), remainingPayment);
        }

        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            logger.warn("Overpayment of {} for payment ID: {}", remainingPayment, payment.getId());
            // TODO: Handle overpayment - could create credit or carry forward to next term
        }

        logger.info("Updated {} dues entries for payment ID: {}", updatedCount, payment.getId());
    }

    @Override
    public String getCommandType() {
        return "UPDATE_DUES_FOR_PAYMENT";
    }
}
