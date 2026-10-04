package com.example.sonata_brain.payment;

import com.example.sonata_brain.access.User;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.payment.PaymentDtos.ConvertRequest;
import com.example.sonata_brain.payment.PaymentDtos.PaymentRequest;
import com.example.sonata_brain.payment.PaymentDtos.PaymentResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestBody(required = false) PaymentRequest request) {
        return payments.create(request);
    }

    @GetMapping
    public List<PaymentResponse> search(@RequestParam(required = false) Long studentId,
                                        @RequestParam(required = false) String courseName,
                                        @RequestParam(required = false) Payment.PaymentType type,
                                        @RequestParam(required = false) Boolean active,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodDate) {
        return payments.search(studentId, courseName, type, active, from, to, periodDate);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id) {
        return payments.get(id);
    }

    @PatchMapping("/{id}/deactivate")
    public PaymentResponse deactivate(@PathVariable Long id) {
        return payments.deactivate(id);
    }

    @PostMapping("/{id}/convert-to-course-fee")
    public PaymentResponse convert(@PathVariable Long id, @RequestBody(required = false) ConvertRequest request,
                                   @AuthenticationPrincipal User user) {
        return payments.convertToCourseFee(id, request == null ? null : request.courseName(),
                request == null ? null : request.periodDate(), user.getEmail());
    }
}
