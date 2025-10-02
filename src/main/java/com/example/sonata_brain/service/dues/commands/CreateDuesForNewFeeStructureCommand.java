package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.repository.FeeStructureRepository;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreateDuesForNewFeeStructureCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(CreateDuesForNewFeeStructureCommand.class);

    private final FeeDuesTrackerService feeDuesTrackerService;
    private final DuesCalculationService duesCalculationService;
    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructure feeStructure;

    public CreateDuesForNewFeeStructureCommand(FeeStructure feeStructure,
                                             FeeDuesTrackerService feeDuesTrackerService,
                                             DuesCalculationService duesCalculationService,
                                             FeeStructureRepository feeStructureRepository) {
        this.feeStructure = feeStructure;
        this.feeDuesTrackerService = feeDuesTrackerService;
        this.duesCalculationService = duesCalculationService;
        this.feeStructureRepository = feeStructureRepository;
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

        if (!feeStructure.getIsActive()) {
            logger.debug("Fee structure is not active, skipping dues creation");
            return;
        }

        // Get all active sessions for the specific term and course
        List<StudentSession> activeSessions = duesCalculationService.getActiveSessionsForYear(feeStructure.getYear());

        int createdCount = 0;

        for (StudentSession session : activeSessions) {
            // Only create dues for sessions in the same course
            if (!session.getCourse().getId().equals(feeStructure.getCourse().getId())) {
                continue;
            }

            // Check if the session overlaps with this fee structure term
            Term feeStructureTerm = new Term(feeStructure.getYear(), feeStructure.getTerm(),
                                           getTermsPerYearFromFeeStructure(feeStructure.getCourse(), feeStructure.getYear()));

            List<Term> sessionTerms = duesCalculationService.calculateDueTermsForSession(session);

            boolean sessionOverlapsWithTerm = sessionTerms.stream()
                .anyMatch(term -> term.getYear() == feeStructureTerm.getYear() &&
                                term.getTermNumber() == feeStructureTerm.getTermNumber());

            if (sessionOverlapsWithTerm) {
                // Check if dues already exist to avoid duplicates
                if (!feeDuesTrackerService.duesExist(session, feeStructure)) {
                    FeeDuesTracker duesTracker = feeDuesTrackerService.createDuesTracker(
                        session, feeStructure, feeStructure.getAmount());

                    createdCount++;
                    logger.debug("Created dues tracker ID: {} for session {} and fee structure {}",
                                duesTracker.getId(), session.getId(), feeStructure.getId());
                } else {
                    logger.debug("Dues already exist for session {} and fee structure {}",
                                session.getId(), feeStructure.getId());
                }
            }
        }

        logger.info("Created {} dues entries for new fee structure ID: {}", createdCount, feeStructure.getId());
    }

    @Override
    public String getCommandType() {
        return "CREATE_DUES_FOR_NEW_FEE_STRUCTURE";
    }
}
