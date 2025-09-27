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

    public static Specification<Student> withDynamicQuery(String name, LocalDate admissionDate, 
            String school, Integer pinCode, String contactNo, String className) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("studentName")),
                    "%" + name.toLowerCase() + "%"
                ));
            }

            if (admissionDate != null) {
                predicates.add(criteriaBuilder.equal(root.get("admissionDate"), admissionDate));
            }

            if (school != null && !school.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("school")),
                    school.toLowerCase()
                ));
            }

            if (pinCode != null) {
                predicates.add(criteriaBuilder.equal(root.get("pinCode"), pinCode));
            }

            if (contactNo != null && !contactNo.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("contactNo"), contactNo));
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
