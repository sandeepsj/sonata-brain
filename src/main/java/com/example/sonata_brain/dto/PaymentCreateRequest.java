package com.example.sonata_brain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class PaymentCreateRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    private LocalDateTime date;

    private String remarks;

    @NotEmpty(message = "Label is required")
    private String label;

    @NotNull(message = "Amount paid is required")
    @PositiveOrZero(message = "Amount paid must be non-negative")
    private BigDecimal amountPaid;

    // Optional - only present for regular payments
    private Long termId;

    // Optional - course name (required for regular payments, optional for additional)
    private String courseName;

    // Optional - discounts associated with this payment
    private List<DiscountCreateRequest> discounts = new ArrayList<>();

    // Custom validation method to check zero amount with discounts
    public boolean isValidZeroPayment() {
        return amountPaid.compareTo(BigDecimal.ZERO) != 0 ||
               (discounts != null && !discounts.isEmpty());
    }

    // Nested class for discount creation
    public static class DiscountCreateRequest {
        @NotNull(message = "Discount amount is required")
        @Positive(message = "Discount amount must be positive (greater than zero)")
        private BigDecimal discountAmount;

        @NotEmpty(message = "Discount label is required")
        private String label;

        private String remarks;

        // Constructors
        public DiscountCreateRequest() {}

        public DiscountCreateRequest(BigDecimal discountAmount, String label, String remarks) {
            this.discountAmount = discountAmount;
            this.label = label;
            this.remarks = remarks;
        }

        // Getters and Setters
        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }

    // Helper method to determine payment type
    public boolean isRegularPayment() {
        return termId != null;
    }

    // Constructors
    public PaymentCreateRequest() {
        this.date = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
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

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Long getTermId() {
        return termId;
    }

    public void setTermId(Long termId) {
        this.termId = termId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public List<DiscountCreateRequest> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(List<DiscountCreateRequest> discounts) {
        this.discounts = discounts;
    }
}
