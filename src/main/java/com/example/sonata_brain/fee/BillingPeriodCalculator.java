package com.example.sonata_brain.fee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns a course's fee timeline into billing periods (REQ-054). Pure: no database, no clock.
 *
 * <p>A structure with N terms per year splits each calendar year into N blocks of 12/N months. A cursor walks
 * forward from the earliest entry: the entry active on the cursor date decides the structure, the period runs to
 * the end of that structure's term containing the cursor, and the next period starts the day after. A cursor in a
 * gap jumps to the next entry's start. A period that starts mid-term is billed at the full term amount (REQ-054.AC4).
 */
public final class BillingPeriodCalculator {

    /** A timeline entry together with the structure it points at. {@code effectiveTo} null means open-ended. */
    public record Slot(LocalDate effectiveFrom, LocalDate effectiveTo, Long feeStructureId, int termsPerYear,
                       Map<Integer, BigDecimal> amountsByTerm) {

        boolean covers(LocalDate day) {
            return !day.isBefore(effectiveFrom) && (effectiveTo == null || !day.isAfter(effectiveTo));
        }
    }

    private BillingPeriodCalculator() {
    }

    /** All periods starting on or before {@code lastStart}. */
    public static List<BillingPeriod> periods(List<Slot> timeline, LocalDate lastStart) {
        List<Slot> slots = timeline.stream().sorted(Comparator.comparing(Slot::effectiveFrom)).toList();
        List<BillingPeriod> periods = new ArrayList<>();
        if (slots.isEmpty()) {
            return periods;
        }
        LocalDate cursor = slots.get(0).effectiveFrom();
        while (!cursor.isAfter(lastStart)) {
            Slot active = activeOn(slots, cursor);
            if (active == null) {
                Slot next = nextAfter(slots, cursor);
                if (next == null) {
                    break;
                }
                cursor = next.effectiveFrom();
                continue;
            }
            int monthsPerTerm = 12 / active.termsPerYear();
            int term = (cursor.getMonthValue() - 1) / monthsPerTerm + 1;
            LocalDate termEnd = YearMonth.of(cursor.getYear(), term * monthsPerTerm).atEndOfMonth();
            BigDecimal amount = active.amountsByTerm().get(term);
            if (amount == null) {
                throw new IllegalStateException("Fee structure " + active.feeStructureId()
                        + " has no amount for term " + term);
            }
            periods.add(new BillingPeriod(cursor, termEnd, term, active.termsPerYear(), amount,
                    active.feeStructureId()));
            cursor = termEnd.plusDays(1);
        }
        return periods;
    }

    /** Periods overlapping {@code [from, to]} (REQ-055). */
    public static List<BillingPeriod> periodsBetween(List<Slot> timeline, LocalDate from, LocalDate to) {
        return periods(timeline, to).stream().filter(p -> p.overlaps(from, to)).toList();
    }

    private static Slot activeOn(List<Slot> slots, LocalDate day) {
        return slots.stream().filter(s -> s.covers(day)).findFirst().orElse(null);
    }

    private static Slot nextAfter(List<Slot> slots, LocalDate day) {
        return slots.stream().filter(s -> s.effectiveFrom().isAfter(day)).findFirst().orElse(null);
    }
}
