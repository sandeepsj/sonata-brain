package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.repository.FeeStructureRepository;
import com.example.sonata_brain.repository.StudentSessionRepository;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class CreateDuesForNewFeeStructureCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(CreateDuesForNewFeeStructureCommand.class);

    private final FeeDuesTrackerService feeDuesTrackerService;
    private final DuesCalculationService duesCalculationService;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final FeeStructure feeStructure;

    public CreateDuesForNewFeeStructureCommand(FeeStructure feeStructure,
                                             FeeDuesTrackerService feeDuesTrackerService,
                                             DuesCalculationService duesCalculationService,
                                             FeeStructureRepository feeStructureRepository,
                                             StudentSessionRepository studentSessionRepository) {
        this.feeStructure = feeStructure;
        this.feeDuesTrackerService = feeDuesTrackerService;
        this.duesCalculationService = duesCalculationService;
        this.feeStructureRepository = feeStructureRepository;
        this.studentSessionRepository = studentSessionRepository;
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
        logger.info("Creating dues for the new fee structure ID: {} (Course: {}, Year: {}, Term: {})",
                   feeStructure.getId(), feeStructure.getCourse().getName(),
                   feeStructure.getYear(), feeStructure.getTerm());

        logger.info("Fee structure isActive status: {}", feeStructure.getIsActive());

        if (!feeStructure.getIsActive()) {
            logger.warn("Fee structure ID {} is not active, skipping dues creation", feeStructure.getId());
            return;
        }

        // Get ALL currently active sessions (not limited to specific year)
        List<StudentSession> allActiveSessions = studentSessionRepository.findAllActiveSessions();

        logger.info("Found {} total active sessions to evaluate", allActiveSessions.size());

        int createdCount = 0;
        int evaluatedCount = 0;

        for (StudentSession session : allActiveSessions) {
            try {
                // Only create dues for sessions in the same course
                if (!session.getCourse().getId().equals(feeStructure.getCourse().getId())) {
                    continue;
                }

                evaluatedCount++;
                logger.info("Evaluating session ID {} for course {}", session.getId(), session.getCourse().getName());

                // For NEW fee structures, create dues for ALL currently active sessions
                // The assumption is: if a student is currently active, they will continue unless they disjoin
                // If they disjoin before the term, the dues will be cancelled automatically

                // Check if session started before or during this fee structure's term
                int termsPerYear = getTermsPerYearFromFeeStructure(feeStructure.getCourse(), feeStructure.getYear());
                Term feeStructureTerm = new Term(feeStructure.getYear(), feeStructure.getTerm(), termsPerYear);
                LocalDateTime termStartDate = feeStructureTerm.convertToDate();

                logger.info("Fee structure term {}/{} starts on: {}",
                           feeStructure.getYear(), feeStructure.getTerm(), termStartDate);
                logger.info("Session {} joined on: {}, disjoined on: {}",
                           session.getId(), session.getDateOfJoining(), session.getDateOfDisjoining());

                // Only create dues if:
                // 1. Student joined before or during the term start
                // 2. Student hasn't disjoined, OR disjoined after the term start
                boolean joinedBeforeTerm = session.getDateOfJoining().isBefore(termStartDate) ||
                                          session.getDateOfJoining().isEqual(termStartDate);
                boolean stillActiveForTerm = session.getDateOfDisjoining() == null ||
                                             session.getDateOfDisjoining().isAfter(termStartDate);

                boolean shouldCreateDues = joinedBeforeTerm && stillActiveForTerm;

                logger.info("Session {} - joinedBeforeTerm: {}, stillActiveForTerm: {}, shouldCreateDues: {}",
                           session.getId(), joinedBeforeTerm, stillActiveForTerm, shouldCreateDues);

                if (shouldCreateDues) {
                    // Check if dues already exist to avoid duplicates
                    if (!feeDuesTrackerService.duesExist(session, feeStructure)) {
                        FeeDuesTracker duesTracker = feeDuesTrackerService.createDuesTracker(
                            session, feeStructure, feeStructure.getAmount());

                        createdCount++;
                        logger.info("Created dues tracker ID: {} for session {} and fee structure {}",
                                    duesTracker.getId(), session.getId(), feeStructure.getId());
                    } else {
                        logger.info("Dues already exist for session {} and fee structure {}",
                                    session.getId(), feeStructure.getId());
                    }
                } else {
                    logger.info("Session {} does not qualify for dues creation for term {}/{}",
                               session.getId(), feeStructure.getYear(), feeStructure.getTerm());
                }
            } catch (Exception e) {
                logger.error("Error processing session ID {}: {}", session.getId(), e.getMessage(), e);
                // Continue with next session instead of failing completely
            }
        }

        logger.info("Created {} dues entries for new fee structure ID: {} (evaluated {} sessions in course)",
                   createdCount, feeStructure.getId(), evaluatedCount);
    }

    @Override
    public String getCommandType() {
        return "CREATE_DUES_FOR_NEW_FEE_STRUCTURE";
    }
}
