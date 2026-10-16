package com.example.sonata_brain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Course {
    public enum Status { ACTIVE, CLOSED }

    @Id
    @Column(name = "name")
    private String name;

    // REQ-041, REQ-042: a closed course takes no new enrolments or course fees
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    @JsonIgnore
    public boolean isClosed() {
        return status == Status.CLOSED;
    }

    public void close() {
        status = Status.CLOSED;
        closedAt = LocalDateTime.now();
    }

    public void reopen() {
        status = Status.ACTIVE;
        closedAt = null;
    }

    /** New courses always start active, whatever the request body says. */
    public void markNew() {
        status = Status.ACTIVE;
        closedAt = null;
    }

    // For backward compatibility, return ID as name
    public String getId() {
        return name;
    }
}
