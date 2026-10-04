package com.example.sonata_brain.fee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Places a fee structure on a course's timeline from one date to another, or open-ended (REQ-053). */
@Entity
@Table(name = "fee_timeline_entry")
public class FeeTimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fee_structure_id")
    private FeeStructure feeStructure;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    protected FeeTimelineEntry() {
    }

    public FeeTimelineEntry(String courseName, FeeStructure feeStructure, LocalDate from, LocalDate to) {
        this.courseName = courseName;
        update(feeStructure, from, to);
    }

    public void update(FeeStructure structure, LocalDate from, LocalDate to) {
        this.feeStructure = structure;
        this.effectiveFrom = from;
        this.effectiveTo = to;
    }

    /** Both ends inclusive; an open end counts as forever. */
    public boolean overlaps(LocalDate from, LocalDate to) {
        boolean startsBeforeOtherEnds = to == null || !effectiveFrom.isAfter(to);
        boolean endsAfterOtherStarts = effectiveTo == null || !effectiveTo.isBefore(from);
        return startsBeforeOtherEnds && endsAfterOtherStarts;
    }

    public BillingPeriodCalculator.Slot slot() {
        return new BillingPeriodCalculator.Slot(effectiveFrom, effectiveTo, feeStructure.getId(),
                feeStructure.getTermsPerYear(), feeStructure.amountsByTerm());
    }

    public Long getId() {
        return id;
    }

    public String getCourseName() {
        return courseName;
    }

    public FeeStructure getFeeStructure() {
        return feeStructure;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }
}
