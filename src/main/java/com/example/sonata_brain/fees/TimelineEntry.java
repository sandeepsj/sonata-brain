package com.example.sonata_brain.fees;

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

/** "From this date, this structure applies" on the school-wide timeline (REQ-058). */
@Entity
@Table(name = "fee_timeline_entry")
public class TimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fee_structure_id")
    private FeeStructure structure;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    protected TimelineEntry() {
    }

    public TimelineEntry(FeeStructure structure, LocalDate effectiveFrom) {
        this.structure = structure;
        this.effectiveFrom = effectiveFrom;
    }

    public Timeline.Entry toEntry() {
        return new Timeline.Entry(effectiveFrom, structure);
    }

    public Long getId() {
        return id;
    }

    public FeeStructure getStructure() {
        return structure;
    }

    public void setStructure(FeeStructure structure) {
        this.structure = structure;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }
}
