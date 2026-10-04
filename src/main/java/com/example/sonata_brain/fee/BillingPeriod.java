package com.example.sonata_brain.fee;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One billable period of a course, produced from the fee timeline (REQ-054). */
public record BillingPeriod(LocalDate periodStart, LocalDate periodEnd, int termNumber, int termsPerYear,
                            BigDecimal amount, Long feeStructureId) {

    public boolean contains(LocalDate day) {
        return !day.isBefore(periodStart) && !day.isAfter(periodEnd);
    }

    public boolean overlaps(LocalDate from, LocalDate to) {
        return !periodStart.isAfter(to) && !periodEnd.isBefore(from);
    }
}
