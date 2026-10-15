package com.example.sonata_brain.access;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Read and write flags on one catalogue path (REQ-011.AC6, AC7). The two flags are independent. */
@Embeddable
public record RouteAccess(
        @Column(name = "path", nullable = false, length = 100) String path,
        @Column(name = "can_read", nullable = false) boolean read,
        @Column(name = "can_write", nullable = false) boolean write) {

    public boolean grantsAnything() {
        return read || write;
    }
}
