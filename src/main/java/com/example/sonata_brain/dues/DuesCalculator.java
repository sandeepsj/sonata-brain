package com.example.sonata_brain.dues;

import com.example.sonata_brain.fees.Timeline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Calculates one student's dues from facts (REQ-070, REQ-071). Pure: the caller passes the terms that have started,
 * the student's sessions and their active REGULAR payments.
 *
 * <ol>
 *   <li>A term is billed for a course when one of the student's sessions in that course overlaps it by at least one
 *       day and the term's structure has an amount for the course. One due per (course, term).</li>
 *   <li>Payments for a course are applied in date order (then id) to the oldest term with a balance: the payment's
 *       discount first, then its amount. Whatever amount is left after the last term is credit; a leftover discount
 *       is not.</li>
 * </ol>
 */
public final class DuesCalculator {

    public record Session(String course, LocalDate from, LocalDate to) {
        boolean overlaps(Timeline.Term term) {
            return !from.isAfter(term.end()) && (to == null || !to.isBefore(term.start()));
        }
    }

    public record Payment(long id, String course, LocalDateTime date, BigDecimal amount, BigDecimal discount) {
    }

    public record Due(String course, Timeline.Term term, BigDecimal amount, BigDecimal paid, BigDecimal discount,
                      BigDecimal balance, DueStatus status) {
    }

    /** Dues by course and term, and the credit per course. */
    public record Result(List<Due> dues, Map<String, BigDecimal> credit) {
    }

    private DuesCalculator() {
    }

    public static Result calculate(List<Timeline.Term> terms, List<Session> sessions, List<Payment> payments) {
        List<Due> dues = new ArrayList<>();
        Map<String, BigDecimal> credit = new TreeMap<>();
        TreeSet<String> courses = new TreeSet<>();
        sessions.forEach(s -> courses.add(s.course()));
        payments.forEach(p -> courses.add(p.course()));

        for (String course : courses) {
            List<Open> open = new ArrayList<>();
            for (Timeline.Term term : terms) {
                Optional<BigDecimal> amount = term.structure().amountFor(course);
                boolean enrolled = sessions.stream().anyMatch(s -> s.course().equals(course) && s.overlaps(term));
                if (amount.isPresent() && enrolled) {
                    open.add(new Open(term, amount.get()));
                }
            }
            BigDecimal leftover = BigDecimal.ZERO;
            List<Payment> ordered = payments.stream().filter(p -> p.course().equals(course))
                    .sorted(Comparator.comparing(Payment::date).thenComparing(Payment::id)).toList();
            for (Payment p : ordered) {
                BigDecimal discount = nz(p.discount());
                BigDecimal cash = nz(p.amount());
                for (Open due : open) {
                    BigDecimal d = discount.min(due.balance());
                    due.discount = due.discount.add(d);
                    discount = discount.subtract(d);
                    BigDecimal c = cash.min(due.balance());
                    due.paid = due.paid.add(c);
                    cash = cash.subtract(c);
                    if (cash.signum() == 0 && discount.signum() == 0) {
                        break;
                    }
                }
                leftover = leftover.add(cash);
            }
            credit.put(course, leftover);
            for (Open due : open) {
                BigDecimal balance = due.balance();
                DueStatus status = balance.signum() == 0 ? DueStatus.PAID
                        : due.paid.add(due.discount).signum() == 0 ? DueStatus.DUE : DueStatus.PARTIALLY_PAID;
                dues.add(new Due(course, due.term, due.amount, due.paid, due.discount, balance, status));
            }
        }
        return new Result(dues, credit);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static final class Open {
        final Timeline.Term term;
        final BigDecimal amount;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        Open(Timeline.Term term, BigDecimal amount) {
            this.term = term;
            this.amount = amount;
        }

        BigDecimal balance() {
            return amount.subtract(paid).subtract(discount);
        }
    }
}
