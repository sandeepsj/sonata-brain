package com.example.sonata_brain.service.dues;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.repository.FeeStructureRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class DuesCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(DuesCalculationService.class);

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private StudentSessionRepository studentSessionRepository;

    /**
     * Calculate all terms that should have dues for a student session
     */
    public List<Term> calculateDueTermsForSession(StudentSession session) {
        List<Term> dueTerms = new ArrayList<>();

        LocalDateTime joinDate = session.getDateOfJoining();
        LocalDateTime endDate = session.getDateOfDisjoining() != null ?
                               session.getDateOfDisjoining() : LocalDateTime.now();

        int startYear = joinDate.getYear();
        int endYear = endDate.getYear();

        logger.debug("Calculating due terms for session {} from {} to {}",
                    session.getId(), startYear, endYear);

        // Get the course to determine terms per year
        Course course = session.getCourse();
        int termsPerYear = course.getTermsPerYear() != null ? course.getTermsPerYear() : 4; // Default to 4 terms

        for (int year = startYear; year <= endYear; year++) {
            for (int termNumber = 1; termNumber <= termsPerYear; termNumber++) {
                Term term = new Term(year, termNumber, termsPerYear);
                LocalDateTime termStartDate = term.convertToDate();
                LocalDateTime termEndDate = term.getTermEndDate();

                // Check if this term overlaps with the student's session period
                if (isTermWithinSessionPeriod(termStartDate, termEndDate, joinDate, endDate)) {
                    dueTerms.add(term);
                }
            }
        }

        logger.debug("Found {} due terms for session {}", dueTerms.size(), session.getId());
        return dueTerms;
    }

    /**
     * Check if a term period overlaps with student session period
     */
    private boolean isTermWithinSessionPeriod(LocalDateTime termStart, LocalDateTime termEnd,
                                            LocalDateTime sessionStart, LocalDateTime sessionEnd) {
        // Term overlaps if: term_start <= session_end AND term_end >= session_start
        return !termStart.isAfter(sessionEnd) && !termEnd.isBefore(sessionStart);
    }

    /**
     * Find fee structure for a specific term and course
     */
    public FeeStructure findFeeStructureForTerm(Course course, int year, int termNumber) {
        return feeStructureRepository.findByCourseAndYearAndTerm(course, year, termNumber)
                .orElse(null);
    }

    /**
     * Get all active student sessions for a specific year
     */
    public List<StudentSession> getActiveSessionsForYear(int year) {
        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(year, 12, 31, 23, 59);

        return studentSessionRepository.findActiveSessionsInPeriod(yearStart, yearEnd);
    }

    /**
     * Calculate terms that should be cancelled after a disjoin date
     */
    public List<Term> calculateTermsToCancel(StudentSession session, LocalDateTime disjoinDate) {
        if (disjoinDate == null) {
            return new ArrayList<>();
        }

        List<Term> termsToCancel = new ArrayList<>();
        Course course = session.getCourse();
        int termsPerYear = course.getTermsPerYear() != null ? course.getTermsPerYear() : 4;

        // Start from disjoin year and check future terms
        int disjoinYear = disjoinDate.getYear();
        int currentYear = LocalDateTime.now().getYear();

        for (int year = disjoinYear; year <= currentYear + 1; year++) { // Check next year too
            for (int termNumber = 1; termNumber <= termsPerYear; termNumber++) {
                Term term = new Term(year, termNumber, termsPerYear);
                LocalDateTime termStartDate = term.convertToDate();

                // If term starts after disjoin date, it should be cancelled
                if (termStartDate.isAfter(disjoinDate)) {
                    termsToCancel.add(term);
                }
            }
        }

        logger.debug("Found {} terms to cancel for session {} after disjoin date {}",
                    termsToCancel.size(), session.getId(), disjoinDate);
        return termsToCancel;
    }

    /**
     * Find matching term for a payment based on fee structure
     */
    public Term getTermFromFeeStructure(FeeStructure feeStructure) {
        if (feeStructure == null) {
            return null;
        }

        Course course = feeStructure.getCourse();
        int termsPerYear = course.getTermsPerYear() != null ? course.getTermsPerYear() : 4;

        return new Term(feeStructure.getYear(), feeStructure.getTerm(), termsPerYear);
    }
}
