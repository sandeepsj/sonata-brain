package com.example.sonata_brain.dues;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One derived due: what a student owes for one billing period of one session (REQ-070). A projection only: the
 * table can be emptied and rebuilt from sessions, the timeline, payments and waivers.
 */
@Entity
@Table(name = "fee_due")
public class FeeDue {

    public enum Status { DUE, PARTIALLY_PAID, PAID, OVERPAID, WAIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_session_id", nullable = false)
    private Long sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "fee_structure_id")
    private Long feeStructureId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "term_number", nullable = false)
    private int termNumber;

    @Column(name = "terms_per_year", nullable = false)
    private int termsPerYear;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(name = "overpaid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal overpaidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    protected FeeDue() {
    }

    public static FeeDue from(DuesProjector.ProjectedDue p) {
        FeeDue due = new FeeDue();
        due.sessionId = p.sessionId();
        due.studentId = p.studentId();
        due.periodStart = p.period().periodStart();
        due.copyFrom(p);
        return due;
    }

    /** True when every computed field already equals the projection. */
    public boolean sameAs(DuesProjector.ProjectedDue p) {
        return Objects.equals(courseName, p.courseName())
                && Objects.equals(feeStructureId, p.period().feeStructureId())
                && periodEnd.equals(p.period().periodEnd())
                && termNumber == p.period().termNumber()
                && termsPerYear == p.period().termsPerYear()
                && amount.compareTo(p.period().amount()) == 0
                && paidAmount.compareTo(p.paid()) == 0
                && discountAmount.compareTo(p.discount()) == 0
                && balance.compareTo(p.balance()) == 0
                && overpaidAmount.compareTo(p.overpaid()) == 0
                && status == p.status();
    }

    public void copyFrom(DuesProjector.ProjectedDue p) {
        courseName = p.courseName();
        feeStructureId = p.period().feeStructureId();
        periodEnd = p.period().periodEnd();
        termNumber = p.period().termNumber();
        termsPerYear = p.period().termsPerYear();
        amount = p.period().amount();
        paidAmount = p.paid();
        discountAmount = p.discount();
        balance = p.balance();
        overpaidAmount = p.overpaid();
        status = p.status();
        computedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getStudentId() { return studentId; }
    public String getCourseName() { return courseName; }
    public Long getFeeStructureId() { return feeStructureId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public int getTermNumber() { return termNumber; }
    public int getTermsPerYear() { return termsPerYear; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getOverpaidAmount() { return overpaidAmount; }
    public Status getStatus() { return status; }
    public LocalDateTime getComputedAt() { return computedAt; }
}
