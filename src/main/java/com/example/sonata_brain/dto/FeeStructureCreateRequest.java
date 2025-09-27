package com.example.sonata_brain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public class FeeStructureCreateRequest {
    @NotNull
    private String courseId;

    @NotNull
    @Min(2000)
    private Integer year;

    @NotNull
    @Min(1)
    private Integer numberOfTerms;

    @NotNull
    @Size(min = 1)
    private List<TermFee> termFees;

    public static class TermFee {
        @NotNull
        @Min(1)
        private Integer term;

        @NotNull
        @Min(0)
        private BigDecimal amount;

        public Integer getTerm() {
            return term;
        }

        public void setTerm(Integer term) {
            this.term = term;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    // Getters and Setters
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getNumberOfTerms() {
        return numberOfTerms;
    }

    public void setNumberOfTerms(Integer numberOfTerms) {
        this.numberOfTerms = numberOfTerms;
    }

    public List<TermFee> getTermFees() {
        return termFees;
    }

    public void setTermFees(List<TermFee> termFees) {
        this.termFees = termFees;
    }
}
