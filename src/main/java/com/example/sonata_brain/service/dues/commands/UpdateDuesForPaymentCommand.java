package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class UpdateDuesForPaymentCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDuesForPaymentCommand.class);

    private final FeeDuesTrackerService feeDuesTrackerService;
    private final DuesCalculationService duesCalculationService;
    private final Payment payment;

    public UpdateDuesForPaymentCommand(Payment payment,
                                     FeeDuesTrackerService feeDuesTrackerService,
                                     DuesCalculationService duesCalculationService) {
        this.payment = payment;
        this.feeDuesTrackerService = feeDuesTrackerService;
        this.duesCalculationService = duesCalculationService;
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

        // Find matching dues for this payment using the correct service method
        List<FeeDuesTracker> matchingDues = feeDuesTrackerService.findMatchingDuesForPayment(
            payment.getStudent().getAdmissionNo(),
            payment.getFeeStructure().getCourse().getName(),
            payment.getFeeStructure().getYear(),
            payment.getFeeStructure().getTerm());

        if (matchingDues.isEmpty()) {
            logger.warn("No matching dues found for payment {} (Student: {}, Fee Structure: {})",
                       payment.getId(), payment.getStudent().getAdmissionNo(), payment.getFeeStructure().getId());
            return;
        }

        for (FeeDuesTracker dues : matchingDues) {
            if (dues.getStatus() == FeeDuesTracker.DuesStatus.DUE ||
                dues.getStatus() == FeeDuesTracker.DuesStatus.PARTIALLY_PAID) {

                // Calculate total effective amount (amountPaid + discounts)
                BigDecimal effectiveAmount = payment.getAmountPaid().add(payment.getDiscountAmount());

                // Use the business method from FeeDuesTracker to update the payment
                dues.updateDueAmount(effectiveAmount, "Payment ID: " + payment.getId());
                feeDuesTrackerService.save(dues);

                logger.debug("Updated dues ID: {} for payment ID: {} with effective amount: {} (paid: {}, discount: {})",
                           dues.getId(), payment.getId(), effectiveAmount, payment.getAmountPaid(), payment.getDiscountAmount());
                break; // Assuming one payment maps to one dues entry
            }
        }

        logger.info("Successfully updated dues for payment ID: {}", payment.getId());
    }

    @Override
    public String getCommandType() {
        return "UPDATE_DUES_FOR_PAYMENT";
    }
}
