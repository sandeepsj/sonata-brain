package com.example.sonata_brain.fees;

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
