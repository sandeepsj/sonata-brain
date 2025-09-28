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
public class RecalculateDuesForDeactivatedPaymentCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(RecalculateDuesForDeactivatedPaymentCommand.class);

    @Autowired
    private FeeDuesTrackerService feeDuesTrackerService;

    @Autowired
    private DuesCalculationService duesCalculationService;

    private final Payment payment;

    public RecalculateDuesForDeactivatedPaymentCommand(Payment payment) {
        this.payment = payment;
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

        // Get the term information from fee structure
        Term term = duesCalculationService.getTermFromFeeStructure(payment.getFeeStructure());
        if (term == null) {
            logger.warn("Could not determine term from fee structure for payment {}", payment.getId());
            return;
        }

        // Find dues that were affected by this payment
        List<FeeDuesTracker> affectedDues = feeDuesTrackerService.findMatchingDuesForPayment(
            payment.getStudent().getAdmissionNo(),
            payment.getCourseName(),
            term.getYear(),
            term.getTermNumber()
        );

        // Also check for paid dues that might have been paid by this payment
        List<FeeDuesTracker> paidDues = feeDuesTrackerService.getDuesByStudentAndStatus(
            payment.getStudent().getAdmissionNo(), FeeDuesTracker.DuesStatus.PAID);

        // Filter paid dues for the same term and course
        paidDues.removeIf(dues ->
            !dues.getTermYear().equals(term.getYear()) ||
            !dues.getTermNumber().equals(term.getTermNumber()) ||
            !dues.getStudentSession().getCourse().getName().equals(payment.getCourseName())
        );

        affectedDues.addAll(paidDues);

        if (affectedDues.isEmpty()) {
            logger.warn("No affected dues found for deactivated payment {}", payment.getId());
            return;
        }

        // Reverse the payment effect - add the payment amount back to dues
        BigDecimal paymentToReverse = payment.getFinalAmount(); // Amount after discounts
        int updatedCount = 0;

        // Reverse in reverse order (LIFO - last applied first)
        for (int i = affectedDues.size() - 1; i >= 0 && paymentToReverse.compareTo(BigDecimal.ZERO) > 0; i--) {
            FeeDuesTracker dues = affectedDues.get(i);

            // Calculate how much was originally paid on this dues entry
            BigDecimal originalFeeAmount = dues.getFeeStructure().getAmount();
            BigDecimal currentDueAmount = dues.getDueAmount();
            BigDecimal paidAmount = originalFeeAmount.subtract(currentDueAmount);

            if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Determine how much to reverse (minimum of what was paid or remaining reversal amount)
                BigDecimal amountToReverse = paymentToReverse.min(paidAmount);

                // Add back the reversed amount
                BigDecimal newDueAmount = dues.getDueAmount().add(amountToReverse);
                dues.setDueAmount(newDueAmount);

                // Update status based on new due amount
                if (newDueAmount.compareTo(originalFeeAmount) >= 0) {
                    dues.setStatus(FeeDuesTracker.DuesStatus.DUE);
                } else if (newDueAmount.compareTo(BigDecimal.ZERO) > 0) {
                    dues.setStatus(FeeDuesTracker.DuesStatus.PARTIALLY_PAID);
                }

                dues.setRemarks("Payment reversed - Payment ID: " + payment.getId());
                feeDuesTrackerService.save(dues);

                paymentToReverse = paymentToReverse.subtract(amountToReverse);
                updatedCount++;

                logger.debug("Reversed {} from payment on dues ID: {}, remaining reversal: {}",
                            amountToReverse, dues.getId(), paymentToReverse);
            }
        }

        if (paymentToReverse.compareTo(BigDecimal.ZERO) > 0) {
            logger.warn("Could not fully reverse payment amount {} for payment ID: {}",
                       paymentToReverse, payment.getId());
        }

        logger.info("Recalculated {} dues entries for deactivated payment ID: {}",
                   updatedCount, payment.getId());
    }

    @Override
    public String getCommandType() {
        return "RECALCULATE_DUES_FOR_DEACTIVATED_PAYMENT";
    }
}
