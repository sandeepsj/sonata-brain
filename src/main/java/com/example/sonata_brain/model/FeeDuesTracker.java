package com.example.sonata_brain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_dues_tracker",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"student_session_id", "fee_structure_id"})
       },
       indexes = {
           @Index(name = "idx_student_session", columnList = "student_session_id"),
           @Index(name = "idx_fee_structure", columnList = "fee_structure_id"),
           @Index(name = "idx_status", columnList = "status")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FeeDuesTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_session_id", nullable = false)
    private StudentSession studentSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @Column(name = "due_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal dueAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DuesStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "remarks")
    private String remarks;

    public enum DuesStatus {
        DUE,
        CANCELLED,
        PAID,
        PARTIALLY_PAID
    }

    // Constructors
    public FeeDuesTracker() {
        this.createdAt = LocalDateTime.now();
        this.status = DuesStatus.DUE;
    }

    public FeeDuesTracker(StudentSession studentSession, FeeStructure feeStructure, BigDecimal dueAmount) {
        this();
        this.studentSession = studentSession;
        this.feeStructure = feeStructure;
        this.dueAmount = dueAmount;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Convenience methods to get term info from fee structure
    public Integer getTermYear() {
        return feeStructure != null ? feeStructure.getYear() : null;
    }

    public Integer getTermNumber() {
        return feeStructure != null ? feeStructure.getTerm() : null;
    }

    public String getCourseName() {
        return feeStructure != null && feeStructure.getCourse() != null ?
               feeStructure.getCourse().getName() : null;
    }

    // Business methods
    public void updateDueAmount(BigDecimal paidAmount, String remarks) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Paid amount cannot be null or negative");
        }

        BigDecimal newDueAmount = this.dueAmount.subtract(paidAmount);

        if (newDueAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.dueAmount = BigDecimal.ZERO;
            this.status = DuesStatus.PAID;
        } else {
            this.dueAmount = newDueAmount;
            this.status = DuesStatus.PARTIALLY_PAID;
        }

        this.remarks = remarks;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        this.status = DuesStatus.CANCELLED;
        this.remarks = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void reactivate(BigDecimal originalAmount) {
        if (this.status == DuesStatus.CANCELLED) {
            this.dueAmount = originalAmount;
            this.status = DuesStatus.DUE;
            this.remarks = "Reactivated";
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean isPaid() {
        return status == DuesStatus.PAID;
    }

    public boolean isActive() {
        return status != DuesStatus.CANCELLED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentSession getStudentSession() {
        return studentSession;
    }

    public void setStudentSession(StudentSession studentSession) {
        this.studentSession = studentSession;
    }

    public FeeStructure getFeeStructure() {
        return feeStructure;
    }

    public void setFeeStructure(FeeStructure feeStructure) {
        this.feeStructure = feeStructure;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }

    public DuesStatus getStatus() {
        return status;
    }

    public void setStatus(DuesStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String toString() {
        return "FeeDuesTracker{" +
                "id=" + id +
                ", termYear=" + getTermYear() +
                ", termNumber=" + getTermNumber() +
                ", dueAmount=" + dueAmount +
                ", status=" + status +
                '}';
    }
}
