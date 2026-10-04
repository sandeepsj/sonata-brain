package com.example.sonata_brain.fee;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A reusable fee template for one course: N terms per year, an amount per term, no dates (REQ-050). */
@Entity
@Table(name = "fee_structure")
public class FeeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "terms_per_year", nullable = false)
    private int termsPerYear;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "feeStructure", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("termNumber")
    private List<FeeStructureTerm> terms = new ArrayList<>();

    protected FeeStructure() {
    }

    public FeeStructure(String name, String courseName) {
        this.name = name;
        this.courseName = courseName;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Replaces the terms. Existing rows are updated in place (inserting a new row for a term number that is still
     * present would collide with the unique (structure, term) key before Hibernate deletes the old one).
     */
    public void setTerms(int termsPerYear, Map<Integer, BigDecimal> amounts) {
        this.termsPerYear = termsPerYear;
        terms.removeIf(t -> !amounts.containsKey(t.getTermNumber()));
        new TreeMap<>(amounts).forEach((term, amount) -> terms.stream()
                .filter(t -> t.getTermNumber() == term)
                .findFirst()
                .ifPresentOrElse(t -> t.setAmount(amount), () -> terms.add(new FeeStructureTerm(this, term, amount))));
        terms.sort((a, b) -> Integer.compare(a.getTermNumber(), b.getTermNumber()));
    }

    public Map<Integer, BigDecimal> amountsByTerm() {
        Map<Integer, BigDecimal> amounts = new TreeMap<>();
        terms.forEach(t -> amounts.put(t.getTermNumber(), t.getAmount()));
        return amounts;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getTermsPerYear() {
        return termsPerYear;
    }

    public List<FeeStructureTerm> getTerms() {
        return terms;
    }
}
