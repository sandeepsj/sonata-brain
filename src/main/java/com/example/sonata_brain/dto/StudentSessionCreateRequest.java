package com.example.sonata_brain.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class StudentSessionCreateRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Course name is required")
    private String courseName;

    private LocalDateTime dateOfJoining;
    private LocalDateTime dateOfDisjoining;

    // Getters and Setters
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
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
