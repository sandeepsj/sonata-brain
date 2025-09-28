package com.example.sonata_brain.service;

import com.example.sonata_brain.model.FeeStructure;
import com.example.sonata_brain.repository.FeeStructureRepository;
import com.example.sonata_brain.service.dues.events.DuesTrackerEventPublisher;
import com.example.sonata_brain.service.dues.events.FeeStructureCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FeeStructureService {

    private static final Logger logger = LoggerFactory.getLogger(FeeStructureService.class);

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private DuesTrackerEventPublisher eventPublisher;

    /**
     * Create a new fee structure and publish dues tracking event
     */
    public FeeStructure createFeeStructure(FeeStructure feeStructure) {
        logger.info("Creating new fee structure for course: {} year: {} term: {}",
                   feeStructure.getCourse().getName(), feeStructure.getYear(), feeStructure.getTerm());

        // Validate unique constraint
        if (feeStructureRepository.existsByCourseAndYear(feeStructure.getCourse(), feeStructure.getYear())) {
            Optional<FeeStructure> existingStructure = feeStructureRepository
                    .findByCourseAndYearAndTerm(feeStructure.getCourse(), feeStructure.getYear(), feeStructure.getTerm());
            if (existingStructure.isPresent()) {
                throw new IllegalArgumentException("Fee structure already exists for this course, year, and term");
            }
        }

        FeeStructure savedFeeStructure = feeStructureRepository.save(feeStructure);

        // Publish event for dues tracking if the fee structure is active
        if (savedFeeStructure.getIsActive()) {
            eventPublisher.publishEvent(new FeeStructureCreatedEvent(this, savedFeeStructure));
            logger.info("Created fee structure ID: {} and published dues tracking event", savedFeeStructure.getId());
        } else {
            logger.info("Created inactive fee structure ID: {} - no dues tracking event published", savedFeeStructure.getId());
        }

        return savedFeeStructure;
    }

    /**
     * Get fee structure by ID
     */
    @Transactional(readOnly = true)
    public Optional<FeeStructure> findById(Long id) {
        return feeStructureRepository.findById(id);
    }

    /**
     * Get fee structures by year
     */
    @Transactional(readOnly = true)
    public List<FeeStructure> getFeeStructuresByYear(Integer year) {
        return feeStructureRepository.findByYear(year);
    }

    /**
     * Get fee structures by course and year
     */
    @Transactional(readOnly = true)
    public List<FeeStructure> getFeeStructuresByCourseAndYear(com.example.sonata_brain.model.Course course, Integer year) {
        return feeStructureRepository.findByCourseAndYear(course, year);
    }

    /**
     * Get fee structure by course, year and term
     */
    @Transactional(readOnly = true)
    public Optional<FeeStructure> getFeeStructureByCourseYearAndTerm(com.example.sonata_brain.model.Course course,
                                                                    Integer year, Integer term) {
        return feeStructureRepository.findByCourseAndYearAndTerm(course, year, term);
    }

    /**
     * Get all fee structures for a course
     */
    @Transactional(readOnly = true)
    public List<FeeStructure> getFeeStructuresByCourse(com.example.sonata_brain.model.Course course) {
        return feeStructureRepository.findByCourse(course);
    }

    /**
     * Get all fee structures
     */
    @Transactional(readOnly = true)
    public List<FeeStructure> getAllFeeStructures() {
        return feeStructureRepository.findAll();
    }

    /**
     * Update fee structure (limited updates allowed)
     */
    public FeeStructure updateFeeStructure(Long id, FeeStructure updatedFeeStructure) {
        logger.info("Updating fee structure ID: {}", id);

        Optional<FeeStructure> existingOpt = feeStructureRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Fee structure not found with ID: " + id);
        }

        FeeStructure existing = existingOpt.get();

        // Only allow certain fields to be updated
        if (updatedFeeStructure.getAmount() != null) {
            existing.setAmount(updatedFeeStructure.getAmount());
        }
        if (updatedFeeStructure.getIsActive() != null) {
            boolean wasActive = existing.getIsActive();
            existing.setIsActive(updatedFeeStructure.getIsActive());

            // If fee structure was inactive and now becomes active, publish event
            if (!wasActive && updatedFeeStructure.getIsActive()) {
                FeeStructure saved = feeStructureRepository.save(existing);
                eventPublisher.publishEvent(new FeeStructureCreatedEvent(this, saved));
                logger.info("Activated fee structure ID: {} and published dues tracking event", saved.getId());
                return saved;
            }
        }

        FeeStructure saved = feeStructureRepository.save(existing);
        logger.info("Updated fee structure ID: {}", saved.getId());
        return saved;
    }

    /**
     * Delete fee structure (soft delete by setting inactive)
     */
    public FeeStructure deactivateFeeStructure(Long id) {
        logger.info("Deactivating fee structure ID: {}", id);

        Optional<FeeStructure> existingOpt = feeStructureRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Fee structure not found with ID: " + id);
        }

        FeeStructure existing = existingOpt.get();
        existing.setIsActive(false);

        FeeStructure saved = feeStructureRepository.save(existing);
        logger.info("Deactivated fee structure ID: {}", saved.getId());
        return saved;
    }
}
