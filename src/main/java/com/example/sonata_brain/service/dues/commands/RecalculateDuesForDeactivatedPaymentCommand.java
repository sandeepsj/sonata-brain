package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class RecalculateDuesForDeactivatedPaymentCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(RecalculateDuesForDeactivatedPaymentCommand.class);

    private final FeeDuesTrackerService feeDuesTrackerService;
    private final DuesCalculationService duesCalculationService;
    private final Payment payment;

    public RecalculateDuesForDeactivatedPaymentCommand(Payment payment,
                                                     FeeDuesTrackerService feeDuesTrackerService,
                                                     DuesCalculationService duesCalculationService) {
        this.payment = payment;
        this.feeDuesTrackerService = feeDuesTrackerService;
        this.duesCalculationService = duesCalculationService;
    }

    @Override
    public void execute() {
        logger.info("Recalculating dues for deactivated payment ID: {} (Student: {}, Amount: {})",
                   payment.getId(), payment.getStudent().getAdmissionNo(), payment.getAmountPaid());

        // Only process regular payments that have fee structure info
        if (payment.getPaymentType() != Payment.PaymentType.REGULAR || payment.getFeeStructure() == null) {
            logger.debug("Skipping non-regular payment or payment without fee structure");
            return;
        }

        // Find dues that were affected by this payment using the correct service method
        List<FeeDuesTracker> affectedDues = feeDuesTrackerService.findMatchingDuesForPayment(
            payment.getStudent().getAdmissionNo(),
            payment.getFeeStructure().getCourse().getName(),
            payment.getFeeStructure().getYear(),
            payment.getFeeStructure().getTerm());

        if (affectedDues.isEmpty()) {
            logger.warn("No dues found for deactivated payment {} (Student: {}, Fee Structure: {})",
                       payment.getId(), payment.getStudent().getAdmissionNo(), payment.getFeeStructure().getId());
            return;
        }

        for (FeeDuesTracker dues : affectedDues) {
            if (dues.getStatus() == FeeDuesTracker.DuesStatus.PAID ||
                dues.getStatus() == FeeDuesTracker.DuesStatus.PARTIALLY_PAID) {

                // Calculate total effective amount that was previously deducted (amountPaid + discounts)
                BigDecimal effectiveAmount = payment.getAmountPaid().add(payment.getDiscountAmount());

                // Add back the effective amount to the dues since payment is now deactivated
                BigDecimal newDueAmount = dues.getDueAmount().add(effectiveAmount);
                dues.setDueAmount(newDueAmount);

                // Update status based on new amount
                BigDecimal totalFeeAmount = payment.getFeeStructure().getAmount();
                if (newDueAmount.compareTo(totalFeeAmount) >= 0) {
                    dues.setStatus(FeeDuesTracker.DuesStatus.DUE);
                } else if (newDueAmount.compareTo(BigDecimal.ZERO) > 0) {
                    dues.setStatus(FeeDuesTracker.DuesStatus.PARTIALLY_PAID);
                }

                dues.setUpdatedAt(java.time.LocalDateTime.now());

                feeDuesTrackerService.save(dues);

                logger.debug("Recalculated dues ID: {} after payment deactivation. Added back effective amount: {} (paid: {}, discount: {})",
                           dues.getId(), effectiveAmount, payment.getAmountPaid(), payment.getDiscountAmount());
                break; // Assuming one payment maps to one dues entry
            }
        }

        logger.info("Successfully recalculated dues for deactivated payment ID: {}", payment.getId());
    }

    @Override
    public String getCommandType() {
        return "RECALCULATE_DUES_FOR_DEACTIVATED_PAYMENT";
    }
}
