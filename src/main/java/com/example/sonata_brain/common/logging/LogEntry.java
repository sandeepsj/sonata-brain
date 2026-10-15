package com.example.sonata_brain.common.logging;

import java.time.Instant;

/** One row of the log (REQ-008). Fields that don't apply to a kind are null. */
public record LogEntry(
        Instant createdAt,
        Kind kind,
        String level,
        String requestId,
        String sessionId,
        String user,
        String method,
        String path,
        String query,
        Integer status,
        Integer durationMs,
        String logger,
        String thread,
        String message,
        String requestBody,
        String responseBody,
        String stackTrace) {

    public enum Kind { REQUEST, APP, ERROR }
}
