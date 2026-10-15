package com.example.sonata_brain.fees;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Term arithmetic of the school-wide timeline (REQ-058, REQ-059). Pure: no database, no clock.
 *
 * <p>An entry with structure S starting on F has terms starting at F + k × S.termMonths (k = 0, 1, …), each ending the
 * day before the next one starts, until the next entry. Month arithmetic always counts from F, so a timeline starting
 * on the 31st gives 31 Jan, 28 Feb, 31 Mar, … without drifting.
 */
public final class Timeline {

    /** One timeline entry as the arithmetic sees it. */
    public record Entry(LocalDate from, FeeStructure structure) {
    }

    /** One billing term. */
    public record Term(LocalDate start, LocalDate end, FeeStructure structure) {
    }

    private Timeline() {
    }

    static LocalDate termStart(LocalDate from, int months, long k) {
        return from.plusMonths(k * months);
    }

    /** Whether {@code date} is the start of a later term (k ≥ 1) of an entry starting on {@code from}. */
    public static boolean isTermStart(LocalDate from, int months, LocalDate date) {
        for (long k = 1; ; k++) {
            LocalDate start = termStart(from, months, k);
            if (!start.isBefore(date)) {
                return start.equals(date);
            }
        }
    }

    /**
     * Checks that every entry starts on a term boundary of the entry before it (REQ-058.AC2). Entries must be sorted
     * by date and have distinct dates. Returns the first problem as a message naming the nearest valid dates.
     */
    public static Optional<String> boundaryProblem(List<Entry> sorted) {
        for (int i = 1; i < sorted.size(); i++) {
            Entry previous = sorted.get(i - 1);
            Entry entry = sorted.get(i);
            int months = previous.structure().getTermMonths();
            if (isTermStart(previous.from(), months, entry.from())) {
                continue;
            }
            LocalDate before = null;
            LocalDate after;
            for (long k = 1; ; k++) {
                LocalDate start = termStart(previous.from(), months, k);
                if (start.isAfter(entry.from())) {
                    after = start;
                    break;
                }
                before = start;
            }
            String nearest = before == null ? after.toString() : before + " or " + after;
            return Optional.of("A fee structure can only start at the beginning of a term. " + entry.from()
                    + " isn't the start of a term of '" + previous.structure().getName() + "' (from "
                    + previous.from() + ", " + months + "-month terms). Nearest term starts: " + nearest + ".");
        }
        return Optional.empty();
    }

    /** The terms of a sorted, valid timeline that overlap {@code [from, to]}, in order. */
    public static List<Term> terms(List<Entry> sorted, LocalDate from, LocalDate to) {
        List<Term> terms = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Entry entry = sorted.get(i);
            LocalDate next = i + 1 < sorted.size() ? sorted.get(i + 1).from() : null;
            int months = entry.structure().getTermMonths();
            for (long k = 0; ; k++) {
                LocalDate start = termStart(entry.from(), months, k);
                if ((next != null && !start.isBefore(next)) || start.isAfter(to)) {
                    break;
                }
                LocalDate following = termStart(entry.from(), months, k + 1);
                if (next != null && following.isAfter(next)) {
                    following = next;
                }
                LocalDate end = following.minusDays(1);
                if (!end.isBefore(from)) {
                    terms.add(new Term(start, end, entry.structure()));
                }
            }
        }
        return terms;
    }
}
