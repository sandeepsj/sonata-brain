package com.example.sonata_brain.fee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_structure_term")
public class FeeStructureTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fee_structure_id")
    private FeeStructure feeStructure;

    @Column(name = "term_number", nullable = false)
    private int termNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    protected FeeStructureTerm() {
    }

    FeeStructureTerm(FeeStructure feeStructure, int termNumber, BigDecimal amount) {
        this.feeStructure = feeStructure;
        this.termNumber = termNumber;
        this.amount = amount;
    }

    public int getTermNumber() {
        return termNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
