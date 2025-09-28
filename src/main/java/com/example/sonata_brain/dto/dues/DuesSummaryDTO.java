package com.example.sonata_brain.dto.dues;

import java.math.BigDecimal;
import java.util.Map;

public class DuesSummaryDTO {

    private Long studentId;
    private String studentName;
    private Integer totalCount;
    private BigDecimal totalDueAmount;
    private Integer dueCount;
    private BigDecimal dueAmount;
    private Integer partiallyPaidCount;
    private BigDecimal partiallyPaidAmount;
    private Integer paidCount;
    private BigDecimal paidAmount;
    private Integer cancelledCount;
    private BigDecimal cancelledAmount;

    // Constructors
    public DuesSummaryDTO() {}

    public DuesSummaryDTO(Long studentId, String studentName, Map<String, Object> summaryData) {
        this.studentId = studentId;
        this.studentName = studentName;

        this.totalCount = (Integer) summaryData.getOrDefault("total_count", 0);
        this.totalDueAmount = (BigDecimal) summaryData.getOrDefault("total_due_amount", BigDecimal.ZERO);

        this.dueCount = ((Long) summaryData.getOrDefault("due_count", 0L)).intValue();
        this.dueAmount = (BigDecimal) summaryData.getOrDefault("due_amount", BigDecimal.ZERO);

        this.partiallyPaidCount = ((Long) summaryData.getOrDefault("partially_paid_count", 0L)).intValue();
        this.partiallyPaidAmount = (BigDecimal) summaryData.getOrDefault("partially_paid_amount", BigDecimal.ZERO);

        this.paidCount = ((Long) summaryData.getOrDefault("paid_count", 0L)).intValue();
        this.paidAmount = (BigDecimal) summaryData.getOrDefault("paid_amount", BigDecimal.ZERO);

        this.cancelledCount = ((Long) summaryData.getOrDefault("cancelled_count", 0L)).intValue();
        this.cancelledAmount = (BigDecimal) summaryData.getOrDefault("cancelled_amount", BigDecimal.ZERO);
    }

    // Getters and Setters
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public BigDecimal getTotalDueAmount() {
        return totalDueAmount;
    }

    public void setTotalDueAmount(BigDecimal totalDueAmount) {
        this.totalDueAmount = totalDueAmount;
    }

    public Integer getDueCount() {
        return dueCount;
    }

    public void setDueCount(Integer dueCount) {
        this.dueCount = dueCount;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }

    public Integer getPartiallyPaidCount() {
        return partiallyPaidCount;
    }

    public void setPartiallyPaidCount(Integer partiallyPaidCount) {
        this.partiallyPaidCount = partiallyPaidCount;
    }

    public BigDecimal getPartiallyPaidAmount() {
        return partiallyPaidAmount;
    }

    public void setPartiallyPaidAmount(BigDecimal partiallyPaidAmount) {
        this.partiallyPaidAmount = partiallyPaidAmount;
    }

    public Integer getPaidCount() {
        return paidCount;
    }

    public void setPaidCount(Integer paidCount) {
        this.paidCount = paidCount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Integer getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(Integer cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public BigDecimal getCancelledAmount() {
        return cancelledAmount;
    }

    public void setCancelledAmount(BigDecimal cancelledAmount) {
        this.cancelledAmount = cancelledAmount;
    }
}
