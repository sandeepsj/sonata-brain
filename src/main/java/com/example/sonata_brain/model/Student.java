package com.example.sonata_brain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admission_no")
    private Long admissionNo;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "contact_no")
    private String contactNo;

    @Column(name = "dob")
    private LocalDate dob;

    // `class` is a Java keyword so use className as the Java field and map to column "class"
    @Column(name = "class")
    private String className;

    @Column(name = "school")
    private String school;

    @Column(name = "address")
    private String address;

    @Column(name = "pin_code")
    private Integer pinCode;

    @Column(name = "remarks")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdmissionStatus status = AdmissionStatus.active;

    public Student() {
    }

    // Minimal constructor for required fields
    public Student(String studentName) {
        this.studentName = studentName;
    }

    // Getters and setters
    public Long getAdmissionNo() {
        return admissionNo;
    }

    public void setAdmissionNo(Long admissionNo) {
        this.admissionNo = admissionNo;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPinCode() {
        return pinCode;
    }

    public void setPinCode(Integer pinCode) {
        this.pinCode = pinCode;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public void setStatus(AdmissionStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(admissionNo, student.admissionNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(admissionNo);
    }

    @Override
    public String toString() {
        return "Student{" +
                "admissionNo=" + admissionNo +
                ", studentName='" + studentName + '\'' +
                ", status=" + status +
                '}';
    }
}

