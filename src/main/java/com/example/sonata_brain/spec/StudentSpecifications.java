package com.example.sonata_brain.spec;

import com.example.sonata_brain.model.AdmissionStatus;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.model.StudentSession;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudentSpecifications {

    public static Specification<Student> withDynamicQuery(String name, LocalDate admissionDateFrom,
            LocalDate admissionDateTo, String school, Integer pinCode, String contactNo, String className, Long admissionNo,
            AdmissionStatus status, String courseName, LocalDateTime now) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (admissionNo != null) {
                predicates.add(criteriaBuilder.equal(root.get("admissionNo"), admissionNo));
            }

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

            // REQ-024.AC1
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // REQ-024.AC2: a session in that course that is active now. EXISTS, so a student with several
            // sessions is returned once, and an unknown course simply matches nobody.
            if (courseName != null && !courseName.trim().isEmpty()) {
                Subquery<Long> active = query.subquery(Long.class);
                Root<StudentSession> session = active.from(StudentSession.class);
                active.select(session.get("id")).where(
                    criteriaBuilder.equal(session.get("student"), root),
                    criteriaBuilder.equal(session.get("course").get("name"), courseName),
                    criteriaBuilder.or(
                        criteriaBuilder.isNull(session.get("dateOfDisjoining")),
                        criteriaBuilder.greaterThan(session.<LocalDateTime>get("dateOfDisjoining"), now)));
                predicates.add(criteriaBuilder.exists(active));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
