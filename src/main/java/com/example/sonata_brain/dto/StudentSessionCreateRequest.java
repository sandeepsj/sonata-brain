package com.example.sonata_brain.dto;

import jakarta.validation.constraints.NotNull;

public class StudentSessionCreateRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Course name is required")
    private String courseName;

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
}
