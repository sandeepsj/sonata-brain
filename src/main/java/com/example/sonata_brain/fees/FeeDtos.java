package com.example.sonata_brain.fees;

import com.example.sonata_brain.dues.DuesDtos.Impact;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the fee-structure and timeline API. */
public final class FeeDtos {

    private FeeDtos() {
    }

    public record AmountRequest(String courseName, BigDecimal amount) {
    }

    public record StructureRequest(String name, Integer termMonths, List<AmountRequest> amounts) {
    }

    public record StructureResponse(Long id, String name, int termMonths, List<FeeAmount> amounts,
                                    boolean usedInTimeline) {
        static StructureResponse of(FeeStructure s, boolean used) {
            return new StructureResponse(s.getId(), s.getName(), s.getTermMonths(), s.sortedAmounts(), used);
        }
    }

    public record EntryRequest(Long feeStructureId, LocalDate effectiveFrom) {
    }

    /** A timeline change and what it did, or would do on a dry run, to the dues (REQ-058.AC10, AC11). */
    public record EntryChange(Long id, Long feeStructureId, String feeStructureName, Integer termMonths,
                              LocalDate effectiveFrom, LocalDate effectiveTo, boolean dryRun, Impact recalculation) {
        static EntryChange of(EntryResponse e, boolean dryRun, Impact impact) {
            return new EntryChange(dryRun ? null : e.id(), e.feeStructureId(), e.feeStructureName(), e.termMonths(),
                    e.effectiveFrom(), e.effectiveTo(), dryRun, impact);
        }

        static EntryChange removal(boolean dryRun, Impact impact) {
            return new EntryChange(null, null, null, null, null, null, dryRun, impact);
        }
    }

    /** A structure edit and, when the structure is on the timeline, its recalculation (REQ-057.AC9). */
    public record StructureChange(Long id, String name, int termMonths, List<FeeAmount> amounts, boolean usedInTimeline,
                                  boolean dryRun, Impact recalculation) {
        static StructureChange of(StructureResponse s, boolean dryRun, Impact impact) {
            return new StructureChange(s.id(), s.name(), s.termMonths(), s.amounts(), s.usedInTimeline(), dryRun,
                    impact);
        }
    }

    public record EntryResponse(Long id, Long feeStructureId, String feeStructureName, int termMonths,
                                LocalDate effectiveFrom, LocalDate effectiveTo) {
    }

    public record PreviewTerm(LocalDate periodStart, LocalDate periodEnd, Long feeStructureId,
                              String feeStructureName, List<FeeAmount> amounts) {
        static PreviewTerm of(Timeline.Term t) {
            return new PreviewTerm(t.start(), t.end(), t.structure().getId(), t.structure().getName(),
                    t.structure().sortedAmounts());
        }
    }
}
