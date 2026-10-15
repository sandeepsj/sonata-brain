package com.example.sonata_brain.controller;

import com.example.sonata_brain.dto.PaymentCreateRequest;
import com.example.sonata_brain.model.Payment;
import com.example.sonata_brain.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        try {
            Payment payment = paymentService.createPayment(request);
            return new ResponseEntity<>(payment, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Return meaningful validation error message
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Validation Failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", 400);
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IllegalStateException e) {
            // Handle state-related errors (like payment already deactivated)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid Operation");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", 409);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (Exception e) {
            log.error("Payment creation failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "An unexpected error occurred while processing the payment");
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", 500);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        try {
            List<Payment> payments = paymentService.getAllPayments();
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        try {
            Optional<Payment> payment = paymentService.getPaymentById(id);
            return payment.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                         .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Payment>> getPaymentsByStudentId(@PathVariable Long studentId) {
        try {
            List<Payment> payments = paymentService.getPaymentsByStudentId(studentId);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/student/{studentId}/type/{paymentType}")
    public ResponseEntity<List<Payment>> getPaymentsByStudentIdAndType(
            @PathVariable Long studentId,
            @PathVariable Payment.PaymentType paymentType) {
        try {
            List<Payment> payments = paymentService.getPaymentsByStudentIdAndType(studentId, paymentType);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/term/{termId}")
    public ResponseEntity<List<Payment>> getPaymentsByTermId(@PathVariable Long termId) {
        try {
            List<Payment> payments = paymentService.getPaymentsByTermId(termId);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivatePayment(@PathVariable Long id) {
        try {
            Payment deactivatedPayment = paymentService.deactivatePayment(id);
            return new ResponseEntity<>(deactivatedPayment, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Payment Not Found");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", 404);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalStateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid Operation");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", 400);
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Payment deactivation failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "An unexpected error occurred while deactivating the payment");
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", 500);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<Payment>> getActivePayments() {
        try {
            List<Payment> payments = paymentService.getActivePayments();
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<Payment>> getInactivePayments() {
        try {
            List<Payment> payments = paymentService.getInactivePayments();
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/student/{studentId}/active")
    public ResponseEntity<List<Payment>> getActivePaymentsByStudentId(@PathVariable Long studentId) {
        try {
            List<Payment> payments = paymentService.getActivePaymentsByStudentId(studentId);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/student/{studentId}/status/{isActive}")
    public ResponseEntity<List<Payment>> getPaymentsByStudentIdAndStatus(
            @PathVariable Long studentId,
            @PathVariable Boolean isActive) {
        try {
            List<Payment> payments = paymentService.getPaymentsByStudentIdAndActiveStatus(studentId, isActive);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Payment>> getPaymentsByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            List<Payment> payments = paymentService.getPaymentsByDateRange(start, end);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/student/{studentId}/date-range")
    public ResponseEntity<List<Payment>> getPaymentsByStudentAndDateRange(
            @PathVariable Long studentId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            List<Payment> payments = paymentService.getPaymentsByStudentAndDateRange(studentId, start, end);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/date-range/course/{courseName}")
    public ResponseEntity<List<Payment>> getPaymentsByDateRangeAndCourse(
            @PathVariable String courseName,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            List<Payment> payments = paymentService.getPaymentsByDateRangeAndCourse(start, end, courseName);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/date-range/type/{paymentType}")
    public ResponseEntity<List<Payment>> getPaymentsByDateRangeAndType(
            @PathVariable Payment.PaymentType paymentType,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            List<Payment> payments = paymentService.getPaymentsByDateRangeAndType(start, end, paymentType);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/date-range/term/{termId}")
    public ResponseEntity<List<Payment>> getPaymentsByDateRangeAndTerm(
            @PathVariable Long termId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            List<Payment> payments = paymentService.getPaymentsByDateRangeAndTerm(start, end, termId);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
