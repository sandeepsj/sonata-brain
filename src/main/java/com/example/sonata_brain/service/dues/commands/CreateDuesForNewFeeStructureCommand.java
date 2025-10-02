package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.repository.FeeStructureRepository;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class CreateDuesForNewFeeStructureCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(CreateDuesForNewFeeStructureCommand.class);

    @Autowired
    private FeeDuesTrackerService feeDuesTrackerService;

    @Autowired
    private DuesCalculationService duesCalculationService;

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    private final FeeStructure feeStructure;

    public CreateDuesForNewFeeStructureCommand(FeeStructure feeStructure) {
        this.feeStructure = feeStructure;
    }

    /**
     * Get number of terms for a course in a specific year from fee structure
     */
    private int getTermsPerYearFromFeeStructure(Course course, int year) {
        List<FeeStructure> feeStructures = feeStructureRepository.findByCourseAndYear(course, year);
        if (feeStructures.isEmpty()) {
            logger.warn("No fee structure found for course {} in year {}, defaulting to 4 terms",
                       course.getName(), year);
            return 4; // Default fallback
        }
        return feeStructures.size(); // Number of fee structure records = number of terms
    }

    @Override
    public void execute() {
        logger.info("Creating dues for new fee structure ID: {} (Course: {}, Year: {}, Term: {})",
                   feeStructure.getId(), feeStructure.getCourse().getName(),
                   feeStructure.getYear(), feeStructure.getTerm());

        // Get all active student sessions for the fee structure's year
        List<StudentSession> activeSessions = duesCalculationService.getActiveSessionsForYear(feeStructure.getYear());

        int createdCount = 0;
        for (StudentSession session : activeSessions) {
            // Only create dues for sessions with the same course
            if (session.getCourse().getId().equals(feeStructure.getCourse().getId())) {
                // Check if student was active during this term
                int termsPerYear = getTermsPerYearFromFeeStructure(session.getCourse(), feeStructure.getYear());
                Term term = new Term(feeStructure.getYear(), feeStructure.getTerm(), termsPerYear);

                if (isStudentActiveInTerm(session, term)) {
                    // Check if dues already exist
                    if (!feeDuesTrackerService.duesExist(session, feeStructure)) {
                        FeeDuesTracker duesTracker = feeDuesTrackerService.createDuesTracker(
                            session, feeStructure, feeStructure.getAmount());

                        createdCount++;
                        logger.debug("Created dues tracker ID: {} for session {}",
                                    duesTracker.getId(), session.getId());
                    } else {
                        logger.debug("Dues already exist for session {} and fee structure {}",
                                    session.getId(), feeStructure.getId());
                    }
                }
            }
        }

        logger.info("Created {} dues entries for new fee structure ID: {}", createdCount, feeStructure.getId());
    }

    private boolean isStudentActiveInTerm(StudentSession session, Term term) {
        // Student is active in term if:
        // 1. Join date is before or during the term
        // 2. Disjoin date is null OR after the term starts

        if (session.getDateOfJoining().isAfter(term.getTermEndDate())) {
            return false; // Joined after term ended
        }

        if (session.getDateOfDisjoining() != null &&
            session.getDateOfDisjoining().isBefore(term.convertToDate())) {
            return false; // Left before term started
        }

        return true;
    }

    @Override
    public String getCommandType() {
        return "CREATE_DUES_FOR_NEW_FEE_STRUCTURE";
    }
}
