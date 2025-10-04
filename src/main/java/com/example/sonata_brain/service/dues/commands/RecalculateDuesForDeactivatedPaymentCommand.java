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

        // Find dues using the fee structure ID directly from the payment (much simpler and accurate)
        List<FeeDuesTracker> affectedDues = feeDuesTrackerService.findByStudentIdAndFeeStructureId(
            payment.getStudent().getAdmissionNo(),
            payment.getFeeStructure().getId());

        if (affectedDues.isEmpty()) {
            logger.warn("No dues found for deactivated payment {} (Student: {}, Fee Structure ID: {})",
                       payment.getId(), payment.getStudent().getAdmissionNo(), payment.getFeeStructure().getId());
            return;
        }

        for (FeeDuesTracker dues : affectedDues) {
            // Calculate total discounts from the payment
            BigDecimal totalDiscounts = payment.getDiscounts().stream()
                .map(discount -> discount.getDiscountAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate total effective amount that was previously deducted (amountPaid + discounts)
            BigDecimal effectiveAmount = payment.getAmountPaid().add(totalDiscounts);

            // Add back the effective amount to the dues since payment is now deactivated
            BigDecimal newDueAmount = dues.getDueAmount().add(effectiveAmount);
            dues.setDueAmount(newDueAmount);

            // Update status based on new amount
            BigDecimal totalFeeAmount = payment.getFeeStructure().getAmount();
            if (newDueAmount.compareTo(totalFeeAmount) >= 0) {
                dues.setStatus(FeeDuesTracker.DuesStatus.DUE);
            } else if (newDueAmount.compareTo(BigDecimal.ZERO) > 0) {
                dues.setStatus(FeeDuesTracker.DuesStatus.PARTIALLY_PAID);
            } else {
                dues.setStatus(FeeDuesTracker.DuesStatus.PAID);
            }

            dues.setUpdatedAt(java.time.LocalDateTime.now());

            feeDuesTrackerService.save(dues);

            logger.debug("Recalculated dues ID: {} after payment deactivation. Added back effective amount: {} (paid: {}, discounts: {})",
                       dues.getId(), effectiveAmount, payment.getAmountPaid(), totalDiscounts);
            break; // One payment maps to one dues entry
        }

        logger.info("Successfully recalculated dues for deactivated payment ID: {}", payment.getId());
    }

    @Override
    public String getCommandType() {
        return "RECALCULATE_DUES_FOR_DEACTIVATED_PAYMENT";
    }
}
