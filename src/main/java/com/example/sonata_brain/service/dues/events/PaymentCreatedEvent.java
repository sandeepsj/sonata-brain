package com.example.sonata_brain.service.dues.events;

import com.example.sonata_brain.model.Payment;

public class PaymentCreatedEvent extends DuesTrackerEvent {

    private final Payment payment;

    public PaymentCreatedEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }

    public Payment getPayment() {
        return payment;
    }

    @Override
    public String getEventType() {
        return "PAYMENT_CREATED";
    }
}
