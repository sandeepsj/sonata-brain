package com.example.sonata_brain.dues;

import com.example.sonata_brain.fee.BillingPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes a student's dues from facts only: sessions, billing periods, active course-fee payments and waivers
 * (REQ-070, REQ-071). Pure: the same inputs always give the same dues.
 *
 * <p>Per course: every billing period overlapping a session becomes a due. Payments are applied in payment-date
 * order. A payment's discount and then its cash first fill the due whose period contains its period date, then the
 * oldest dues of that course with something left to pay; anything still left stays on the latest due as overpaid.
 * Waived dues take no money.
 */
public final class DuesProjector {

    public record SessionFact(Long sessionId, Long studentId, String courseName, LocalDate joined, LocalDate left) {
    }

    public record PaymentFact(Long paymentId, String courseName, LocalDate periodDate, LocalDateTime date,
                              BigDecimal cash, BigDecimal discount) {
    }

    public record WaiverKey(Long sessionId, LocalDate periodStart) {
    }

    public record ProjectedDue(Long sessionId, Long studentId, String courseName, BillingPeriod period,
                               BigDecimal paid, BigDecimal discount, BigDecimal balance, BigDecimal overpaid,
                               FeeDue.Status status) {
    }

    private DuesProjector() {
    }

    public static List<ProjectedDue> project(List<SessionFact> sessions, Map<String, List<BillingPeriod>> periodsByCourse,
                                             List<PaymentFact> payments, Set<WaiverKey> waivers) {
        List<ProjectedDue> result = new ArrayList<>();
        sessions.stream().map(SessionFact::courseName).distinct().sorted().forEach(course -> {
            List<Slot> slots = new ArrayList<>();
            for (SessionFact s : sessions) {
                if (!s.courseName().equals(course)) {
                    continue;
                }
                for (BillingPeriod p : periodsByCourse.getOrDefault(course, List.of())) {
                    LocalDate end = s.left() == null ? LocalDate.MAX : s.left();
                    if (p.overlaps(s.joined(), end)) {
                        slots.add(new Slot(s, p, waivers.contains(new WaiverKey(s.sessionId(), p.periodStart()))));
                    }
                }
            }
            slots.sort(Comparator.comparing((Slot x) -> x.period.periodStart()).thenComparing(x -> x.session.sessionId()));
            payments.stream()
                    .filter(p -> p.courseName().equals(course))
                    .sorted(Comparator.comparing(PaymentFact::date).thenComparing(PaymentFact::paymentId))
                    .forEach(p -> apply(p, slots));
            slots.forEach(slot -> result.add(slot.toDue()));
        });
        return result;
    }

    private static void apply(PaymentFact payment, List<Slot> slots) {
        Slot target = slots.stream()
                .filter(s -> !s.waived && s.period.contains(payment.periodDate()))
                .findFirst().orElse(null);
        distribute(payment.discount(), true, target, slots);
        distribute(payment.cash(), false, target, slots);
    }

    private static void distribute(BigDecimal money, boolean isDiscount, Slot target, List<Slot> slots) {
        BigDecimal left = money == null ? BigDecimal.ZERO : money;
        if (left.signum() <= 0) {
            return;
        }
        if (target != null) {
            left = target.take(left, isDiscount);
        }
        for (Slot slot : slots) {
            if (left.signum() <= 0) {
                return;
            }
            if (!slot.waived) {
                left = slot.take(left, isDiscount);
            }
        }
        if (left.signum() > 0) {
            // Nothing left to pay anywhere in this course: keep the money visible on the latest due (REQ-071.AC3).
            BigDecimal rest = left;
            slots.stream().filter(s -> !s.waived).reduce((a, b) -> b)
                    .ifPresent(last -> last.add(rest, isDiscount));
        }
    }

    private static final class Slot {
        final SessionFact session;
        final BillingPeriod period;
        final boolean waived;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        Slot(SessionFact session, BillingPeriod period, boolean waived) {
            this.session = session;
            this.period = period;
            this.waived = waived;
        }

        BigDecimal capacity() {
            return period.amount().subtract(paid).subtract(discount).max(BigDecimal.ZERO);
        }

        /** Takes what fits; returns what is left over. */
        BigDecimal take(BigDecimal money, boolean isDiscount) {
            BigDecimal taken = money.min(capacity());
            add(taken, isDiscount);
            return money.subtract(taken);
        }

        void add(BigDecimal money, boolean isDiscount) {
            if (isDiscount) {
                discount = discount.add(money);
            } else {
                paid = paid.add(money);
            }
        }

        ProjectedDue toDue() {
            BigDecimal amount = period.amount();
            BigDecimal applied = paid.add(discount);
            FeeDue.Status status;
            BigDecimal balance;
            BigDecimal overpaid = BigDecimal.ZERO;
            if (waived) {
                status = FeeDue.Status.WAIVED;
                balance = BigDecimal.ZERO;
            } else if (applied.compareTo(amount) > 0) {
                status = FeeDue.Status.OVERPAID;
                balance = BigDecimal.ZERO;
                overpaid = applied.subtract(amount);
            } else if (applied.compareTo(amount) == 0) {
                status = FeeDue.Status.PAID;
                balance = BigDecimal.ZERO;
            } else {
                status = applied.signum() == 0 ? FeeDue.Status.DUE : FeeDue.Status.PARTIALLY_PAID;
                balance = amount.subtract(applied);
            }
            return new ProjectedDue(session.sessionId(), session.studentId(), session.courseName(), period,
                    paid, discount, balance, overpaid, status);
        }
    }
}
