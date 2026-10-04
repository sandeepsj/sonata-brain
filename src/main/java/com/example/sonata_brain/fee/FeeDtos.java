package com.example.sonata_brain.fee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class FeeDtos {

    private FeeDtos() {
    }

    public record TermAmount(Integer termNumber, BigDecimal amount) {
    }

    public record FeeStructureRequest(String name, String courseName, Integer termsPerYear, List<TermAmount> terms) {
    }

    public record CloneRequest(String name) {
    }

    public record FeeStructureResponse(Long id, String name, String courseName, int termsPerYear,
                                       List<TermAmount> terms, boolean usedInTimeline) {
        static FeeStructureResponse of(FeeStructure s, boolean used) {
            List<TermAmount> terms = s.amountsByTerm().entrySet().stream()
                    .map(e -> new TermAmount(e.getKey(), e.getValue()))
                    .toList();
            return new FeeStructureResponse(s.getId(), s.getName(), s.getCourseName(), s.getTermsPerYear(), terms, used);
        }
    }

    public record TimelineRequest(String courseName, Long feeStructureId, LocalDate effectiveFrom,
                                  LocalDate effectiveTo) {
    }

    public record TimelineResponse(Long id, String courseName, Long feeStructureId, String feeStructureName,
                                   LocalDate effectiveFrom, LocalDate effectiveTo) {
        static TimelineResponse of(FeeTimelineEntry e) {
            return new TimelineResponse(e.getId(), e.getCourseName(), e.getFeeStructure().getId(),
                    e.getFeeStructure().getName(), e.getEffectiveFrom(), e.getEffectiveTo());
        }
    }
}
