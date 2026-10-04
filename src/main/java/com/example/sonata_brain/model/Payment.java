package com.example.sonata_brain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Money received. The source of truth for payments: never deleted, only deactivated (REQ-064).
 * A REGULAR payment pays a course fee for the billing period containing {@code periodDate} (REQ-060).
 */
@Entity
@Table(name = "payments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment {

    public enum PaymentType { REGULAR, ADDITIONAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "period_date")
    private LocalDate periodDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_type")
    private PaymentType originalType;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "converted_by")
    private String convertedBy;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("payment")
    private List<Discount> discounts = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (date == null) {
            date = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Cash plus discounts: what this payment settles. */
    public BigDecimal settledAmount() {
        return amountPaid.add(discountAmount == null ? BigDecimal.ZERO : discountAmount);
    }

    /** Turns an ADDITIONAL payment into a course fee, keeping an audit trail (REQ-065). */
    public void convertToCourseFee(String course, LocalDate period, String by) {
        originalType = paymentType;
        paymentType = PaymentType.REGULAR;
        courseName = course;
        periodDate = period;
        convertedAt = LocalDateTime.now();
        convertedBy = by;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public LocalDate getPeriodDate() { return periodDate; }
    public void setPeriodDate(LocalDate periodDate) { this.periodDate = periodDate; }
    public PaymentType getOriginalType() { return originalType; }
    public LocalDateTime getConvertedAt() { return convertedAt; }
    public String getConvertedBy() { return convertedBy; }
    public List<Discount> getDiscounts() { return discounts; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
