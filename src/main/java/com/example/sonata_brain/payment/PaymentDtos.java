package com.example.sonata_brain.payment;

import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record DiscountRequest(BigDecimal discountAmount, String label, String remarks) {
    }

    public record PaymentRequest(Long studentId, Payment.PaymentType paymentType, String courseName,
                                 LocalDate periodDate, BigDecimal amountPaid, String label, String remarks,
                                 LocalDateTime date, List<DiscountRequest> discounts) {
    }

    public record ConvertRequest(String courseName, LocalDate periodDate) {
    }

    public record DiscountResponse(Long id, BigDecimal discountAmount, String label, String remarks) {
        static DiscountResponse of(Discount d) {
            return new DiscountResponse(d.getId(), d.getDiscountAmount(), d.getLabel(), d.getRemarks());
        }
    }

    public record PaymentResponse(Long id, Long studentId, String studentName, Payment.PaymentType paymentType,
                                  String courseName, LocalDate periodDate, BigDecimal amountPaid,
                                  BigDecimal discountAmount, List<DiscountResponse> discounts, String label,
                                  String remarks, LocalDateTime date, Boolean isActive,
                                  Payment.PaymentType originalType, LocalDateTime convertedAt, String convertedBy,
                                  LocalDateTime createdAt) {
        static PaymentResponse of(Payment p) {
            return new PaymentResponse(p.getId(), p.getStudent().getAdmissionNo(), p.getStudent().getStudentName(),
                    p.getPaymentType(), p.getCourseName(), p.getPeriodDate(), p.getAmountPaid(), p.getDiscountAmount(),
                    p.getDiscounts().stream().sorted(Comparator.comparing(Discount::getId))
                            .map(DiscountResponse::of).toList(),
                    p.getLabel(), p.getRemarks(), p.getDate(), p.getIsActive(), p.getOriginalType(),
                    p.getConvertedAt(), p.getConvertedBy(), p.getCreatedAt());
        }
    }
}
