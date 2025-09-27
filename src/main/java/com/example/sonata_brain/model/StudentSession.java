package com.example.sonata_brain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "student_sessions")
public class StudentSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_name", referencedColumnName = "courseName", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @Column(name = "date_of_joining", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime dateOfJoining;

    @Column(name = "date_of_disjoining")
    private LocalDateTime dateOfDisjoining;

    @PrePersist
    @PreUpdate
    private void validateDates() {
        if (dateOfDisjoining != null && dateOfDisjoining.isBefore(dateOfJoining)) {
            throw new IllegalStateException("Date of disjoining must be after date of joining");
        }
    }

    public boolean isActive() {
        return dateOfDisjoining == null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public LocalDateTime getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDateTime dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public LocalDateTime getDateOfDisjoining() {
        return dateOfDisjoining;
    }

    public void setDateOfDisjoining(LocalDateTime dateOfDisjoining) {
        this.dateOfDisjoining = dateOfDisjoining;
    }
}
