package com.example.sonata_brain.fees;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A school-wide fee structure: a term length and an amount per course. It has no dates (REQ-057). */
@Entity
@Table(name = "fee_structure")
public class FeeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fee_structure_amount", joinColumns = @JoinColumn(name = "fee_structure_id"))
    private Set<FeeAmount> amounts = new HashSet<>();

    protected FeeStructure() {
    }

    public FeeStructure(String name, int termMonths, Collection<FeeAmount> amounts) {
        this.name = name;
        this.termMonths = termMonths;
        this.amounts.addAll(amounts);
    }

    /** The per-term amount for a course, or empty when the course isn't billed under this structure. */
    public Optional<BigDecimal> amountFor(String courseName) {
        return amounts.stream().filter(a -> a.courseName().equals(courseName)).map(FeeAmount::amount).findFirst();
    }

    public List<FeeAmount> sortedAmounts() {
        return amounts.stream().sorted(Comparator.comparing(FeeAmount::courseName)).toList();
    }

    public void replace(String name, int termMonths, Collection<FeeAmount> amounts) {
        this.name = name;
        this.termMonths = termMonths;
        this.amounts.clear();
        this.amounts.addAll(amounts);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTermMonths() {
        return termMonths;
    }
}
