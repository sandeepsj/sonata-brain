package com.example.sonata_brain.repository;

import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findByPaymentId(Long paymentId);

    List<Discount> findByPayment(Payment payment);

    void deleteByPaymentId(Long paymentId);
}
