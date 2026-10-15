package com.example.sonata_brain.dues;

import com.example.sonata_brain.fees.FeeAmount;
import com.example.sonata_brain.fees.FeeStructure;
import com.example.sonata_brain.fees.Timeline;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuesCalculatorTest {

    private static final FeeStructure STANDARD = new FeeStructure("Standard", 3, List.of(
            new FeeAmount("Violin", new BigDecimal("1000")), new FeeAmount("Piano", new BigDecimal("1200"))));
    private static final FeeStructure REVISED = new FeeStructure("Revised", 3, List.of(
            new FeeAmount("Violin", new BigDecimal("1200"))));

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private static List<Timeline.Term> terms(Timeline.Entry... entries) {
        return Timeline.terms(List.of(entries), d("2025-01-01"), d("2025-12-31"));
    }

    private static DuesCalculator.Payment pay(long id, String date, String amount, String discount) {
        return new DuesCalculator.Payment(id, "Violin", LocalDateTime.parse(date + "T10:00:00"), new BigDecimal(amount),
                new BigDecimal(discount));
    }

    private static final DuesCalculator.Session ASHA = new DuesCalculator.Session("Violin", d("2025-01-10"), d("2025-12-20"));

    @Test
    void billsEveryTermTheSessionTouchesAndAppliesPaymentsOldestFirst() {
        var result = DuesCalculator.calculate(terms(new Timeline.Entry(d("2025-01-01"), STANDARD)), List.of(ASHA),
                List.of(pay(2, "2025-06-01", "1500", "0"), pay(1, "2025-02-01", "700", "0")));
        assertThat(result.dues()).extracting(DuesCalculator.Due::status).containsExactly(
                DueStatus.PAID, DueStatus.PAID, DueStatus.PARTIALLY_PAID, DueStatus.DUE);
        assertThat(result.dues().get(2).balance()).isEqualByComparingTo("800");
        assertThat(result.credit().get("Violin")).isEqualByComparingTo("0");
    }

    @Test
    void aFeeChangeOnlyAffectsLaterTerms() {
        var payments = List.of(pay(1, "2025-05-01", "2500", "0"));
        var before = DuesCalculator.calculate(terms(new Timeline.Entry(d("2025-01-01"), STANDARD)), List.of(ASHA), payments);
        var after = DuesCalculator.calculate(terms(new Timeline.Entry(d("2025-01-01"), STANDARD),
                new Timeline.Entry(d("2025-10-01"), REVISED)), List.of(ASHA), payments);
        assertThat(after.dues().subList(0, 3)).isEqualTo(before.dues().subList(0, 3));
        assertThat(after.dues().get(3).amount()).isEqualByComparingTo("1200");
        assertThat(after.dues().get(3).term().structure()).isSameAs(REVISED);
    }

    @Test
    void midTermJoinAndLeaveBillTheWholeTermAndNothingAfter() {
        var session = new DuesCalculator.Session("Piano", d("2025-02-15"), d("2025-05-10"));
        var result = DuesCalculator.calculate(terms(new Timeline.Entry(d("2025-01-01"), STANDARD)), List.of(session), List.of());
        assertThat(result.dues()).extracting(due -> due.term().start()).containsExactly(d("2025-01-01"), d("2025-04-01"));
        assertThat(result.dues()).allSatisfy(due -> assertThat(due.amount()).isEqualByComparingTo("1200"));
    }

    @Test
    void discountsAreAppliedFirstAndOnlyLeftoverCashIsCredit() {
        var result = DuesCalculator.calculate(terms(new Timeline.Entry(d("2025-01-01"), STANDARD)), List.of(ASHA),
                List.of(pay(1, "2025-02-01", "4500", "300")));
        assertThat(result.dues()).allSatisfy(due -> assertThat(due.status()).isEqualTo(DueStatus.PAID));
        assertThat(result.dues().get(0).discount()).isEqualByComparingTo("300");
        assertThat(result.dues().get(0).paid()).isEqualByComparingTo("700");
        assertThat(result.credit().get("Violin")).isEqualByComparingTo("800");
    }

    @Test
    void aZeroPaymentWithADiscountForgivesTheOldestOpenTerm() {
        var result = DuesCalculator.calculate(terms(new Timeline.Entry(d("2025-01-01"), STANDARD)), List.of(ASHA),
                List.of(pay(1, "2025-11-01", "0", "1000")));
        assertThat(result.dues().get(0).status()).isEqualTo(DueStatus.PAID);
        assertThat(result.dues().get(0).paid()).isEqualByComparingTo("0");
        assertThat(result.dues().get(1).status()).isEqualTo(DueStatus.DUE);
    }

    @Test
    void aCourseWithoutAnAmountIsNotBilled() {
        var guitar = new DuesCalculator.Session("Guitar", d("2025-01-05"), null);
        var result = DuesCalculator.calculate(terms(new Timeline.Entry(d("2025-01-01"), STANDARD)), List.of(guitar), List.of());
        assertThat(result.dues()).isEmpty();
    }

    @Test
    void calculatingTwiceGivesTheSameResult() {
        var t = terms(new Timeline.Entry(d("2025-01-01"), STANDARD));
        var p = List.of(pay(1, "2025-02-01", "1300", "100"));
        assertThat(DuesCalculator.calculate(t, List.of(ASHA), p)).isEqualTo(DuesCalculator.calculate(t, List.of(ASHA), p));
    }
}
