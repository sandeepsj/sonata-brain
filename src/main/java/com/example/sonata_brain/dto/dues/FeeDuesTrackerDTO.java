package com.example.sonata_brain.dto.dues;

import com.example.sonata_brain.model.FeeDuesTracker;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FeeDuesTrackerDTO {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long sessionId;
    private String courseName;
    private Long feeStructureId;
    private Integer termYear;
    private Integer termNumber;
    private BigDecimal dueAmount;
    private FeeDuesTracker.DuesStatus status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public FeeDuesTrackerDTO() {}

    public FeeDuesTrackerDTO(FeeDuesTracker duesTracker) {
        this.id = duesTracker.getId();
        this.studentId = duesTracker.getStudentSession().getStudent().getAdmissionNo();
        this.studentName = duesTracker.getStudentSession().getStudent().getStudentName();
        this.sessionId = duesTracker.getStudentSession().getId();
        this.courseName = duesTracker.getStudentSession().getCourse().getName();
        this.feeStructureId = duesTracker.getFeeStructure().getId();
        this.termYear = duesTracker.getTermYear();
        this.termNumber = duesTracker.getTermNumber();
        this.dueAmount = duesTracker.getDueAmount();
        this.status = duesTracker.getStatus();
        this.remarks = duesTracker.getRemarks();
        this.createdAt = duesTracker.getCreatedAt();
        this.updatedAt = duesTracker.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Long getFeeStructureId() {
        return feeStructureId;
    }

    public void setFeeStructureId(Long feeStructureId) {
        this.feeStructureId = feeStructureId;
    }

    public Integer getTermYear() {
        return termYear;
    }

    public void setTermYear(Integer termYear) {
        this.termYear = termYear;
    }

    public Integer getTermNumber() {
        return termNumber;
    }

    public void setTermNumber(Integer termNumber) {
        this.termNumber = termNumber;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }

    public FeeDuesTracker.DuesStatus getStatus() {
        return status;
    }

    public void setStatus(FeeDuesTracker.DuesStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
}
