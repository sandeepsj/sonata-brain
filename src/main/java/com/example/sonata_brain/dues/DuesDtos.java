package com.example.sonata_brain.dues;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Response bodies of the dues API (REQ-070.AC7, REQ-074.AC8, REQ-076.AC7). */
public final class DuesDtos {

    private DuesDtos() {
    }

    public record DueResponse(Long id, Long studentId, String studentName, String courseName, LocalDate periodStart,
                              LocalDate periodEnd, Long feeStructureId, String feeStructureName, BigDecimal amount,
                              BigDecimal paidAmount, BigDecimal discountAmount, BigDecimal balance, DueStatus status) {
        static DueResponse of(FeeDue d) {
            return new DueResponse(d.getId(), d.getStudent().getAdmissionNo(), d.getStudent().getStudentName(),
                    d.getCourseName(), d.getPeriodStart(), d.getPeriodEnd(), d.getStructure().getId(),
                    d.getStructure().getName(), d.getAmount(), d.getPaidAmount(), d.getDiscountAmount(),
                    d.getBalance(), d.getStatus());
        }
    }

    public record CourseSummary(String courseName, BigDecimal amount, BigDecimal paid, BigDecimal discount,
                                BigDecimal balance, BigDecimal credit) {
    }

    public record Summary(Long studentId, List<CourseSummary> courses, BigDecimal amount, BigDecimal paid,
                          BigDecimal discount, BigDecimal balance, BigDecimal credit) {
    }

    /** What one recalculation changed. */
    public record Changes(int inserted, int updated, int deleted) {
        Changes plus(Changes o) {
            return new Changes(inserted + o.inserted, updated + o.updated, deleted + o.deleted);
        }
    }

    /**
     * What a recalculation of everyone changes (REQ-058.AC10): students whose dues change, due rows inserted, updated
     * and deleted, and the total outstanding balance before and after.
     */
    public record Impact(int students, int inserted, int updated, int deleted, BigDecimal outstandingBefore,
                         BigDecimal outstandingAfter, BigDecimal outstandingChange, long durationMs) {
        Changes changes() {
            return new Changes(inserted, updated, deleted);
        }
    }

    public record Job(String jobId, String scope, String status, int students, Changes changes, List<String> errors,
                      LocalDateTime startedAt, LocalDateTime finishedAt) {
    }
}
