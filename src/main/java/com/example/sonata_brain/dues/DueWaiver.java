package com.example.sonata_brain.dues;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A decision to waive one period of one session. Survives rebuilds (REQ-072). */
@Entity
@Table(name = "due_waiver")
public class DueWaiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_session_id", nullable = false)
    private Long sessionId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private String reason;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected DueWaiver() {
    }

    public DueWaiver(Long sessionId, LocalDate periodStart, String reason, String createdBy) {
        this.sessionId = sessionId;
        this.periodStart = periodStart;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public String getReason() { return reason; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
