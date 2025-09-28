package com.example.sonata_brain.service.payment;

import com.example.sonata_brain.dto.PaymentCreateRequest;
import com.example.sonata_brain.model.Payment;

public interface PaymentStrategy {
    Payment processPayment(PaymentCreateRequest request);
    void validatePaymentRequest(PaymentCreateRequest request);
}
