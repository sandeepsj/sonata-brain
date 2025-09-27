package com.example.sonata_brain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "student_sessions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_name", referencedColumnName = "name")
    private Course course;

    @Column(name = "date_of_joining", nullable = false, updatable = false)
    private LocalDateTime dateOfJoining;

    @Column(name = "date_of_disjoining")
    private LocalDateTime dateOfDisjoining;

    // Constructor for JPA
    public StudentSession() {
        this.dateOfJoining = LocalDateTime.now();
    }

    // Factory method for creating new sessions with optional dates
    public static StudentSession createSession(Student student, Course course, LocalDateTime joiningDate, LocalDateTime disjoinDate) {
        StudentSession session = new StudentSession();
        session.setStudent(student);
        session.setCourse(course);

        if (joiningDate != null) {
            session.setDateOfJoining(joiningDate);
        }

        if (disjoinDate != null) {
            if (disjoinDate.isBefore(session.getDateOfJoining())) {
                throw new IllegalArgumentException("Date of disjoining must be after date of joining");
            }
            session.setDateOfDisjoining(disjoinDate);
        }

        return session;
    }

    // Overloaded method for backward compatibility
    public static StudentSession createSession(Student student, Course course) {
        return createSession(student, course, null, null);
    }

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
