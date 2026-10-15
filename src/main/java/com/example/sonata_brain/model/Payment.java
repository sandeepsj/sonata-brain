package com.example.sonata_brain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment {
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

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("payment")
    private List<Discount> discounts = new ArrayList<>();

    // REQ-065.AC8: set when an ADDITIONAL payment is converted into a course fee
    @Enumerated(EnumType.STRING)
    @Column(name = "original_type")
    private PaymentType originalType;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "converted_by")
    private String convertedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentType {
        REGULAR,
        ADDITIONAL
    }

    // Constructors
    public Payment() {
        this.date = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Calculate total discount amount from associated discounts
    public BigDecimal calculateTotalDiscountAmount() {
        return discounts.stream()
                .map(Discount::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Calculate final amount after discount - now based on amount_paid minus discounts
    public BigDecimal getFinalAmount() {
        return amountPaid.subtract(calculateTotalDiscountAmount());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public List<Discount> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(List<Discount> discounts) {
        this.discounts = discounts;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public PaymentType getOriginalType() {
        return originalType;
    }

    public LocalDateTime getConvertedAt() {
        return convertedAt;
    }

    public String getConvertedBy() {
        return convertedBy;
    }

    /** Turns this ADDITIONAL payment into a course fee for {@code course}, recording who did it and when. */
    public void convertToCourseFee(String course, String by) {
        this.originalType = this.paymentType;
        this.paymentType = PaymentType.REGULAR;
        this.courseName = course;
        this.convertedAt = LocalDateTime.now();
        this.convertedBy = by;
    }
}
