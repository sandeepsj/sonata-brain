package com.example.sonata_brain.controller;

import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/discounts")
@CrossOrigin(origins = "*")
public class DiscountController {

    @Autowired
    private DiscountService discountService;

    @GetMapping
    public ResponseEntity<List<Discount>> getAllDiscounts() {
        try {
            List<Discount> discounts = discountService.getAllDiscounts();
            return new ResponseEntity<>(discounts, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Discount> getDiscountById(@PathVariable Long id) {
        try {
            Optional<Discount> discount = discountService.getDiscountById(id);
            return discount.map(d -> new ResponseEntity<>(d, HttpStatus.OK))
                         .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<Discount>> getDiscountsByPaymentId(@PathVariable Long paymentId) {
        try {
            List<Discount> discounts = discountService.getDiscountsByPaymentId(paymentId);
            return new ResponseEntity<>(discounts, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
