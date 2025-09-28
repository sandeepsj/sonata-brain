package com.example.sonata_brain.service;

import com.example.sonata_brain.model.Discount;
import com.example.sonata_brain.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DiscountService {

    @Autowired
    private DiscountRepository discountRepository;

    public List<Discount> getDiscountsByPaymentId(Long paymentId) {
        return discountRepository.findByPaymentId(paymentId);
    }

    public Optional<Discount> getDiscountById(Long discountId) {
        return discountRepository.findById(discountId);
    }

    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }
}
