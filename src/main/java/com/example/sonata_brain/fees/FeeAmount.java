package com.example.sonata_brain.fees;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/** What one course costs per term under a fee structure (REQ-057). */
@Embeddable
public record FeeAmount(
        @Column(name = "course_name", nullable = false) String courseName,
        @Column(name = "amount", nullable = false, precision = 10, scale = 2) BigDecimal amount) {
}
