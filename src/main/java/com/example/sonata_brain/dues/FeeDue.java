package com.example.sonata_brain.dues;

import com.example.sonata_brain.fees.FeeStructure;
import com.example.sonata_brain.model.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** A calculated due: one per (student, course, term). Written only by {@link DuesService} (REQ-070). */
@Entity
@Table(name = "fee_due")
public class FeeDue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fee_structure_id")
    private FeeStructure structure;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DueStatus status;

    protected FeeDue() {
    }

    FeeDue(Student student, String courseName, LocalDate periodStart) {
        this.student = student;
        this.courseName = courseName;
        this.periodStart = periodStart;
    }

    /** Copies the calculated values; returns whether anything changed. */
    boolean apply(DuesCalculator.Due due) {
        boolean changed = !Objects.equals(periodEnd, due.term().end())
                || structure == null || !Objects.equals(structure.getId(), due.term().structure().getId())
                || differs(amount, due.amount()) || differs(paidAmount, due.paid())
                || differs(discountAmount, due.discount()) || differs(balance, due.balance()) || status != due.status();
        periodEnd = due.term().end();
        structure = due.term().structure();
        amount = due.amount();
        paidAmount = due.paid();
        discountAmount = due.discount();
        balance = due.balance();
        status = due.status();
        return changed;
    }

    private static boolean differs(BigDecimal a, BigDecimal b) {
        return a == null || b == null || a.compareTo(b) != 0;
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public String getCourseName() {
        return courseName;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public FeeStructure getStructure() {
        return structure;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public DueStatus getStatus() {
        return status;
    }
}
