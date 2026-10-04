package com.example.sonata_brain.dues;

/**
 * Signals that some dues must be recomputed. They carry only ids: the rebuild always reloads committed data, never
 * entities shared with the request thread (REQ-064.AC7).
 */
public final class DuesEvents {

    private DuesEvents() {
    }

    /** A student's sessions or payments changed (REQ-074.AC1). */
    public record StudentDuesInvalidated(Long studentId) {
    }

    /** A course's timeline, or a structure on it, changed (REQ-074.AC2). */
    public record CourseDuesInvalidated(String courseName) {
    }
}
