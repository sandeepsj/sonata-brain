package com.example.sonata_brain.service;

import com.example.sonata_brain.model.FeeDuesTracker;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.model.FeeStructure;
import com.example.sonata_brain.repository.FeeDuesTrackerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class FeeDuesTrackerService {

    @Autowired
    private FeeDuesTrackerRepository feeDuesTrackerRepository;

    // Create new dues tracker entry
    public FeeDuesTracker createDuesTracker(StudentSession studentSession, FeeStructure feeStructure, BigDecimal dueAmount) {
        // Check if dues already exist for this combination
        if (feeDuesTrackerRepository.existsByStudentSessionAndFeeStructure(studentSession, feeStructure)) {
            throw new IllegalArgumentException("Dues already exist for this session and fee structure");
        }

        FeeDuesTracker duesTracker = new FeeDuesTracker(studentSession, feeStructure, dueAmount);
        return feeDuesTrackerRepository.save(duesTracker);
    }

    // Get all dues for a student
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getDuesByStudentId(Long studentId) {
        return feeDuesTrackerRepository.findByStudentIdOrderByTermYearAscTermNumberAsc(studentId);
    }

    // Get dues for a specific year
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getDuesByYear(Integer year) {
        return feeDuesTrackerRepository.findByTermYearOrderByTermNumberAscStudentSessionAsc(year);
    }

    // Get dues for a specific term
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getDuesByTerm(Integer year, Integer termNumber) {
        return feeDuesTrackerRepository.findByTermYearAndTermNumber(year, termNumber);
    }

    // Get dues for a course
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getDuesByCourse(String courseName) {
        return feeDuesTrackerRepository.findByCourseNameOrderByTermYearAscTermNumberAsc(courseName);
    }

    // Get dues for a student session
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getDuesBySession(StudentSession session) {
        return feeDuesTrackerRepository.findByStudentSessionOrderByFeeStructure_YearAscFeeStructure_TermAsc(session);
    }

    // Get dues summary for a student
    @Transactional(readOnly = true)
    public Map<String, Object> getDuesSummary(Long studentId) {
        List<Object[]> summaryData = feeDuesTrackerRepository.getDuesSummaryByStudentId(studentId);
        Map<String, Object> summary = new HashMap<>();

        BigDecimal totalDue = BigDecimal.ZERO;
        int totalCount = 0;

        for (Object[] row : summaryData) {
            FeeDuesTracker.DuesStatus status = (FeeDuesTracker.DuesStatus) row[0];
            Long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            summary.put(status.toString().toLowerCase() + "_count", count);
            summary.put(status.toString().toLowerCase() + "_amount", amount);

            totalCount += count.intValue();
            if (status == FeeDuesTracker.DuesStatus.DUE || status == FeeDuesTracker.DuesStatus.PARTIALLY_PAID) {
                totalDue = totalDue.add(amount);
            }
        }

        summary.put("total_count", totalCount);
        summary.put("total_due_amount", totalDue);

        return summary;
    }

    // Update dues amount (typically called when payment is made)
    public FeeDuesTracker updateDuesAmount(Long duesId, BigDecimal paidAmount, String remarks) {
        Optional<FeeDuesTracker> optionalDues = feeDuesTrackerRepository.findById(duesId);
        if (optionalDues.isEmpty()) {
            throw new IllegalArgumentException("Dues not found with ID: " + duesId);
        }

        FeeDuesTracker dues = optionalDues.get();
        dues.updateDueAmount(paidAmount, remarks);
        return feeDuesTrackerRepository.save(dues);
    }

    // Cancel dues (typically called when student disjoins)
    public List<FeeDuesTracker> cancelDuesAfterTerm(StudentSession session, Integer year, Integer termNumber, String reason) {
        List<FeeDuesTracker> duesToCancel = feeDuesTrackerRepository.findByStudentSessionAfterTerm(session, year, termNumber);

        for (FeeDuesTracker dues : duesToCancel) {
            if (dues.isActive()) {
                dues.cancel(reason);
            }
        }

        return feeDuesTrackerRepository.saveAll(duesToCancel);
    }

    // Find matching dues for a payment
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> findMatchingDuesForPayment(Long studentId, String courseName,
                                                          Integer termYear, Integer termNumber) {
        return feeDuesTrackerRepository.findMatchingDuesForPayment(studentId, courseName, termYear, termNumber);
    }

    // Get active dues only
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getActiveDues() {
        return feeDuesTrackerRepository.findActiveDues();
    }

    // Get dues by status
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getDuesByStatus(FeeDuesTracker.DuesStatus status) {
        return feeDuesTrackerRepository.findByStatusOrderByTermYearAscTermNumberAsc(status);
    }

    // Get dues by student and status
    @Transactional(readOnly = true)
    public List<FeeDuesTracker> getDuesByStudentAndStatus(Long studentId, FeeDuesTracker.DuesStatus status) {
        return feeDuesTrackerRepository.findByStudentIdAndStatus(studentId, status);
    }

    // Check if dues exist for a session and fee structure
    @Transactional(readOnly = true)
    public boolean duesExist(StudentSession session, FeeStructure feeStructure) {
        return feeDuesTrackerRepository.existsByStudentSessionAndFeeStructure(session, feeStructure);
    }

    // Save dues tracker
    public FeeDuesTracker save(FeeDuesTracker duesTracker) {
        return feeDuesTrackerRepository.save(duesTracker);
    }

    // Save multiple dues trackers
    public List<FeeDuesTracker> saveAll(List<FeeDuesTracker> duesTrackers) {
        return feeDuesTrackerRepository.saveAll(duesTrackers);
    }

    // Find by ID
    @Transactional(readOnly = true)
    public Optional<FeeDuesTracker> findById(Long id) {
        return feeDuesTrackerRepository.findById(id);
    }

    // Delete dues tracker
    public void delete(Long id) {
        feeDuesTrackerRepository.deleteById(id);
    }
}
