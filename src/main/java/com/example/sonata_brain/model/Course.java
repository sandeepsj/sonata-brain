package com.example.sonata_brain.model;

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
    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "terms_per_year")
    private Integer termsPerYear;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTermsPerYear() {
        return termsPerYear;
    }

    public void setTermsPerYear(Integer termsPerYear) {
        this.termsPerYear = termsPerYear;
    }

    // For backward compatibility, return ID as name
    public String getId() {
        return name;
    }
}
