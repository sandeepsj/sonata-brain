package com.example.sonata_brain.spec;

import com.example.sonata_brain.model.Student;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentSpecifications {

    public static Specification<Student> withDynamicQuery(String name, LocalDate admissionDateFrom,
            LocalDate admissionDateTo, String school, Integer pinCode, String contactNo, String className) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("studentName")),
                    "%" + name.toLowerCase() + "%"
                ));
            }

            if (admissionDateFrom != null && admissionDateTo != null) {
                predicates.add(criteriaBuilder.between(root.get("admissionDate"), admissionDateFrom, admissionDateTo));
            } else if (admissionDateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("admissionDate"), admissionDateFrom));
            } else if (admissionDateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("admissionDate"), admissionDateTo));
            }

            if (school != null && !school.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("school")),
                    "%" + school.toLowerCase() + "%"
                ));
            }

            if (pinCode != null) {
                predicates.add(criteriaBuilder.equal(root.get("pinCode"), pinCode));
            }

            if (contactNo != null && !contactNo.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    root.get("contactNo"),
                    "%" + contactNo + "%"
                ));
            }

            if (className != null && !className.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("className")),
                    className.toLowerCase()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
