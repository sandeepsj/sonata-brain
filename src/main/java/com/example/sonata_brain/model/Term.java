package com.example.sonata_brain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

public class Term {
    private int year;
    private int termNumber;
    private int totalTermsInYear;

    public Term() {}

    public Term(int year, int termNumber, int totalTermsInYear) {
        this.year = year;
        this.termNumber = termNumber;
        this.totalTermsInYear = totalTermsInYear;
    }

    // Convert term to date - each term starts at the beginning of its period
    public LocalDateTime convertToDate() {
        if (termNumber < 1 || termNumber > totalTermsInYear) {
            throw new IllegalArgumentException("Invalid term number: " + termNumber + " for " + totalTermsInYear + " total terms");
        }

        // Calculate months per term
        int monthsPerTerm = 12 / totalTermsInYear;

        // Calculate starting month for this term (0-based)
        int startMonth = (termNumber - 1) * monthsPerTerm;

        // Convert to Month enum (1-based)
        Month month = Month.of(startMonth + 1);

        return LocalDateTime.of(year, month, 1, 0, 0);
    }

    // Get the end date of the term
    public LocalDateTime getTermEndDate() {
        if (termNumber < 1 || termNumber > totalTermsInYear) {
            throw new IllegalArgumentException("Invalid term number: " + termNumber + " for " + totalTermsInYear + " total terms");
        }

        int monthsPerTerm = 12 / totalTermsInYear;
        int startMonth = (termNumber - 1) * monthsPerTerm;
        int endMonth = startMonth + monthsPerTerm - 1;

        Month month = Month.of(endMonth + 1);

        // Get the last day of the end month
        LocalDate endDate = LocalDate.of(year, month, month.length(LocalDate.of(year, month, 1).isLeapYear()));
        return endDate.atTime(23, 59, 59);
    }

    // Getters and Setters
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTermNumber() {
        return termNumber;
    }

    public void setTermNumber(int termNumber) {
        this.termNumber = termNumber;
    }

    public int getTotalTermsInYear() {
        return totalTermsInYear;
    }

    public void setTotalTermsInYear(int totalTermsInYear) {
        this.totalTermsInYear = totalTermsInYear;
    }

    @Override
    public String toString() {
        return "Term{" +
                "year=" + year +
                ", termNumber=" + termNumber +
                ", totalTermsInYear=" + totalTermsInYear +
                '}';
    }
}
