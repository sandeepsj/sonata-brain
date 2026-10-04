package com.example.sonata_brain.access;

/** Access level on a resource. WRITE implies READ (REQ-011.AC2). NONE is never stored. */
public enum Access {
    NONE,
    READ,
    WRITE;

    public Access max(Access other) {
        return compareTo(other) >= 0 ? this : other;
    }
}
