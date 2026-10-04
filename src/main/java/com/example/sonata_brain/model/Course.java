package com.example.sonata_brain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/** A subject taught at the institute. Identified by its name. Can be closed and reopened (REQ-042). */
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Course {

    public enum Status { ACTIVE, CLOSED }

    @Id
    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    protected Course() {
    }

    public Course(String name) {
        this.name = name;
    }

    public boolean isClosed() {
        return status == Status.CLOSED;
    }

    public void close(LocalDateTime at) {
        status = Status.CLOSED;
        closedAt = at;
    }

    public void reopen() {
        status = Status.ACTIVE;
        closedAt = null;
    }

    public String getName() {
        return name;
    }

    /** Same as the name; kept because older code and responses use it. */
    public String getId() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
