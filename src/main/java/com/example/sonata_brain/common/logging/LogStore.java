package com.example.sonata_brain.common.logging;

/**
 * Where log entries go (REQ-008). The only implementation for now is {@link DatabaseLogStore}.
 * Implementations must never throw and must not block the caller for long: logging must not fail a request.
 */
public interface LogStore {

    void write(LogEntry entry);
}
