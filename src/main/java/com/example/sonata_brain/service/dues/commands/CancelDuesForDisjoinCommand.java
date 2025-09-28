package com.example.sonata_brain.service.dues.commands;

import com.example.sonata_brain.model.*;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Scope("prototype")
public class CancelDuesForDisjoinCommand implements DuesTrackerCommand {

    private static final Logger logger = LoggerFactory.getLogger(CancelDuesForDisjoinCommand.class);

    @Autowired
    private FeeDuesTrackerService feeDuesTrackerService;

    @Autowired
    private DuesCalculationService duesCalculationService;

    private final StudentSession studentSession;
    private final LocalDateTime disjoinDate;

    public CancelDuesForDisjoinCommand(StudentSession studentSession, LocalDateTime disjoinDate) {
        this.studentSession = studentSession;
        this.disjoinDate = disjoinDate;
    }

    @Override
    public void execute() {
        logger.info("Cancelling dues for student session ID: {} after disjoin date: {}",
                   studentSession.getId(), disjoinDate);

        if (disjoinDate == null) {
            logger.debug("No disjoin date provided, skipping dues cancellation");
            return;
        }

        // Calculate which terms should be cancelled
        List<Term> termsToCancel = duesCalculationService.calculateTermsToCancel(studentSession, disjoinDate);

        int cancelledCount = 0;

        // Get all existing dues for this session
        List<FeeDuesTracker> existingDues = feeDuesTrackerService.getDuesBySession(studentSession);

        for (FeeDuesTracker dues : existingDues) {
            // Check if this dues entry is for a term that should be cancelled
            for (Term termToCancel : termsToCancel) {
                if (dues.getTermYear().equals(termToCancel.getYear()) &&
                    dues.getTermNumber().equals(termToCancel.getTermNumber())) {

                    // Only cancel if not already paid
                    if (dues.getStatus() != FeeDuesTracker.DuesStatus.PAID &&
                        dues.getStatus() != FeeDuesTracker.DuesStatus.CANCELLED) {

                        dues.cancel("Student disjoined on " + disjoinDate);
                        feeDuesTrackerService.save(dues);
                        cancelledCount++;

                        logger.debug("Cancelled dues ID: {} for term {}-{}",
                                    dues.getId(), dues.getTermYear(), dues.getTermNumber());
                    }
                    break;
                }
            }
        }

        logger.info("Cancelled {} dues entries for student session ID: {} after disjoin",
                   cancelledCount, studentSession.getId());
    }

    @Override
    public String getCommandType() {
        return "CANCEL_DUES_FOR_DISJOIN";
    }
}
