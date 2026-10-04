package com.example.sonata_brain.repository;

import com.example.sonata_brain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStudentAdmissionNoAndPaymentTypeAndIsActive(Long admissionNo, Payment.PaymentType type,
                                                                    Boolean isActive);

    List<Payment> findByStudentAdmissionNo(Long admissionNo);
}
