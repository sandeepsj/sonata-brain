package com.example.sonata_brain.repository;

import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStudentAdmissionNo(Long studentAdmissionNo);

    List<Payment> findByStudentAdmissionNoAndPaymentType(Long studentAdmissionNo, Payment.PaymentType paymentType);

    @Query("SELECT p FROM Payment p WHERE p.student.admissionNo = :studentAdmissionNo AND p.date BETWEEN :startDate AND :endDate AND p.isActive = true ORDER BY p.date DESC")
    List<Payment> findByStudentAdmissionNoAndDateBetween(@Param("studentAdmissionNo") Long studentAdmissionNo,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // Find all payments within a date range
    @Query("SELECT p FROM Payment p WHERE p.date BETWEEN :startDate AND :endDate AND p.isActive = true ORDER BY p.date DESC")
    List<Payment> findByDateBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // Find payments by date range and course name
    @Query("SELECT p FROM Payment p WHERE p.date BETWEEN :startDate AND :endDate AND p.courseName = :courseName AND p.isActive = true ORDER BY p.date DESC")
    List<Payment> findByDateBetweenAndCourseName(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("courseName") String courseName);

    // Find payments by date range and payment type
    @Query("SELECT p FROM Payment p WHERE p.date BETWEEN :startDate AND :endDate AND p.paymentType = :paymentType AND p.isActive = true ORDER BY p.date DESC")
    List<Payment> findByDateBetweenAndPaymentType(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  @Param("paymentType") Payment.PaymentType paymentType);

    // Find payments by date range and term (fee structure)
    @Query("SELECT p FROM Payment p WHERE p.date BETWEEN :startDate AND :endDate AND p.feeStructure.id = :termId AND p.isActive = true ORDER BY p.date DESC")
    List<Payment> findByDateBetweenAndTermId(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("termId") Long termId);

    @Query("SELECT p FROM Payment p WHERE p.feeStructure.id = :termId")
    List<Payment> findByTermId(@Param("termId") Long termId);

    @Query("SELECT p FROM Payment p WHERE p.courseName = :courseName")
    List<Payment> findByCourseName(@Param("courseName") String courseName);

    // Methods to filter by active status
    List<Payment> findByIsActive(Boolean isActive);

    List<Payment> findByStudentAdmissionNoAndIsActive(Long studentAdmissionNo, Boolean isActive);

    @Query("SELECT p FROM Payment p WHERE p.student.admissionNo = :studentAdmissionNo AND p.paymentType = :paymentType AND p.isActive = :isActive")
    List<Payment> findByStudentAdmissionNoAndPaymentTypeAndIsActive(@Param("studentAdmissionNo") Long studentAdmissionNo,
                                                                   @Param("paymentType") Payment.PaymentType paymentType,
                                                                   @Param("isActive") Boolean isActive);

    // Method to find existing payments for a student and specific term (fee structure)
    @Query("SELECT p FROM Payment p WHERE p.student.admissionNo = :studentAdmissionNo AND p.feeStructure.id = :termId AND p.paymentType = 'REGULAR' AND p.isActive = true")
    List<Payment> findActiveRegularPaymentsByStudentAndTerm(@Param("studentAdmissionNo") Long studentAdmissionNo, @Param("termId") Long termId);
}
