package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreateDuesForNewSessionCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(CreateDuesForNewSessionCommand.class);

    private final FeeDuesTrackerService feeDuesTrackerService;
    private final DuesCalculationService duesCalculationService;
    private final StudentSession studentSession;

    public CreateDuesForNewSessionCommand(StudentSession studentSession,
                                        FeeDuesTrackerService feeDuesTrackerService,
                                        DuesCalculationService duesCalculationService) {
        this.studentSession = studentSession;
        this.feeDuesTrackerService = feeDuesTrackerService;
        this.duesCalculationService = duesCalculationService;
    }

    @Override
    public void execute() {
        logger.info("Creating dues for new session ID: {}", studentSession.getId());

        // Calculate all terms that need dues for this session
        List<Term> dueTerms = duesCalculationService.calculateDueTermsForSession(studentSession);

        int createdCount = 0;
        for (Term term : dueTerms) {
            // Find corresponding fee structure
            FeeStructure feeStructure = duesCalculationService.findFeeStructureForTerm(
                studentSession.getCourse(), term.getYear(), term.getTermNumber());

            if (feeStructure != null && feeStructure.getIsActive()) {
                // Check if dues already exist to avoid duplicates
                if (!feeDuesTrackerService.duesExist(studentSession, feeStructure)) {
                    FeeDuesTracker duesTracker = feeDuesTrackerService.createDuesTracker(
                        studentSession, feeStructure, feeStructure.getAmount());

                    createdCount++;
                    logger.debug("Created dues tracker ID: {} for term {}-{}",
                                duesTracker.getId(), term.getYear(), term.getTermNumber());
                } else {
                    logger.debug("Dues already exist for session {} and fee structure {}",
                                studentSession.getId(), feeStructure.getId());
                }
            } else {
                logger.debug("No active fee structure found for course {} year {} term {}",
                            studentSession.getCourse().getName(), term.getYear(), term.getTermNumber());
            }
        }

        logger.info("Created {} dues entries for session ID: {}", createdCount, studentSession.getId());
    }

    @Override
    public String getCommandType() {
        return "CREATE_DUES_FOR_NEW_SESSION";
    }
}
