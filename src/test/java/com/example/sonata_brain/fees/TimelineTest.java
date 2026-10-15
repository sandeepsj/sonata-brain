package com.example.sonata_brain.fees;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineTest {

    private static FeeStructure structure(String name, int months) {
        return new FeeStructure(name, months, List.of(new FeeAmount("Violin", new BigDecimal("1000"))));
    }

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    @Test
    void termsCountFromTheEntryAndStopAtTheNextOne() {
        var q = structure("Quarterly", 3);
        var m = structure("Monthly", 1);
        var terms = Timeline.terms(List.of(new Timeline.Entry(d("2025-01-01"), q), new Timeline.Entry(d("2025-07-01"), m)),
                d("2025-01-01"), d("2025-08-15"));
        assertThat(terms).extracting(Timeline.Term::start)
                .containsExactly(d("2025-01-01"), d("2025-04-01"), d("2025-07-01"), d("2025-08-01"));
        assertThat(terms.get(1).end()).isEqualTo(d("2025-06-30"));
        assertThat(terms.get(2).structure()).isSameAs(m);
    }

    @Test
    void termsStartingOnThe31stDontDrift() {
        var terms = Timeline.terms(List.of(new Timeline.Entry(d("2025-01-31"), structure("Monthly", 1))),
                d("2025-01-01"), d("2025-04-15"));
        assertThat(terms).extracting(Timeline.Term::start)
                .containsExactly(d("2025-01-31"), d("2025-02-28"), d("2025-03-31"));
        assertThat(terms).extracting(Timeline.Term::end)
                .containsExactly(d("2025-02-27"), d("2025-03-30"), d("2025-04-29"));
    }

    @Test
    void termsOnlyOverlappingTheRangeAreReturned() {
        var terms = Timeline.terms(List.of(new Timeline.Entry(d("2025-01-01"), structure("Q", 3))),
                d("2025-05-15"), d("2025-07-01"));
        assertThat(terms).extracting(Timeline.Term::start).containsExactly(d("2025-04-01"), d("2025-07-01"));
    }

    @Test
    void aNewStructureMustStartOnATermBoundary() {
        var q = structure("Quarterly", 3);
        var r = structure("Revised", 3);
        assertThat(Timeline.boundaryProblem(List.of(new Timeline.Entry(d("2025-01-01"), q),
                new Timeline.Entry(d("2025-10-01"), r)))).isEmpty();
        assertThat(Timeline.boundaryProblem(List.of(new Timeline.Entry(d("2025-01-01"), q),
                new Timeline.Entry(d("2025-10-15"), r))))
                .hasValueSatisfying(msg -> assertThat(msg).contains("2025-10-01").contains("2026-01-01"));
        assertThat(Timeline.isTermStart(d("2025-01-01"), 3, d("2025-01-01"))).isFalse();
        assertThat(Timeline.isTermStart(d("2025-01-31"), 1, d("2025-02-28"))).isTrue();
    }
}
