package com.example.sonata_brain.service.dues.events;

import com.example.sonata_brain.service.dues.commands.*;
import com.example.sonata_brain.service.FeeDuesTrackerService;
import com.example.sonata_brain.service.dues.DuesCalculationService;
import com.example.sonata_brain.repository.FeeStructureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DuesEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(DuesEventHandler.class);

    private final DuesCommandProcessor commandProcessor;
    private final FeeDuesTrackerService feeDuesTrackerService;
    private final DuesCalculationService duesCalculationService;
    private final FeeStructureRepository feeStructureRepository;

    public DuesEventHandler(DuesCommandProcessor commandProcessor,
                           FeeDuesTrackerService feeDuesTrackerService,
                           DuesCalculationService duesCalculationService,
                           FeeStructureRepository feeStructureRepository) {
        this.commandProcessor = commandProcessor;
        this.feeDuesTrackerService = feeDuesTrackerService;
        this.duesCalculationService = duesCalculationService;
        this.feeStructureRepository = feeStructureRepository;
    }

    @EventListener
    @Async
    @Transactional
    public void handleStudentSessionCreated(StudentSessionCreatedEvent event) {
        logger.info("Handling student session created event for session ID: {}",
                   event.getStudentSession().getId());

        try {
            CreateDuesForNewSessionCommand command = new CreateDuesForNewSessionCommand(
                event.getStudentSession(), feeDuesTrackerService, duesCalculationService);
            commandProcessor.process(command);
            logger.info("Successfully created dues for new session ID: {}",
                       event.getStudentSession().getId());
        } catch (Exception e) {
            logger.error("Error creating dues for new session ID: {}",
                        event.getStudentSession().getId(), e);
            throw e;
        }
    }

    @EventListener
    @Async
    @Transactional
    public void handleStudentSessionUpdated(StudentSessionUpdatedEvent event) {
        logger.info("Handling student session updated event for session ID: {}",
                   event.getStudentSession().getId());

        // Only process if disjoin date changed
        if (!event.isDisjoinDateChanged()) {
            logger.debug("Disjoin date not changed for session ID: {}, skipping dues update",
                        event.getStudentSession().getId());
            return;
        }

        try {
            CancelDuesForDisjoinCommand command = new CancelDuesForDisjoinCommand(
                event.getStudentSession(), event.getNewDisjoinDate(),
                feeDuesTrackerService, duesCalculationService);
            commandProcessor.process(command);
            logger.info("Successfully updated dues for session disjoin ID: {}",
                       event.getStudentSession().getId());
        } catch (Exception e) {
            logger.error("Error updating dues for session disjoin ID: {}",
                        event.getStudentSession().getId(), e);
            throw e;
        }
    }

    @EventListener
    @Async
    @Transactional
    public void handleFeeStructureCreated(FeeStructureCreatedEvent event) {
        logger.info("Handling fee structure created event for structure ID: {}",
                   event.getFeeStructure().getId());

        try {
            CreateDuesForNewFeeStructureCommand command = new CreateDuesForNewFeeStructureCommand(
                event.getFeeStructure(), feeDuesTrackerService, duesCalculationService, feeStructureRepository);
            commandProcessor.process(command);
            logger.info("Successfully created dues for new fee structure ID: {}",
                       event.getFeeStructure().getId());
        } catch (Exception e) {
            logger.error("Error creating dues for new fee structure ID: {}",
                        event.getFeeStructure().getId(), e);
            throw e;
        }
    }

    @EventListener
    @Async
    @Transactional
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        logger.info("Handling payment created event for payment ID: {}",
                   event.getPayment().getId());

        try {
            UpdateDuesForPaymentCommand command = new UpdateDuesForPaymentCommand(
                event.getPayment(), feeDuesTrackerService, duesCalculationService);
            commandProcessor.process(command);
            logger.info("Successfully updated dues for new payment ID: {}",
                       event.getPayment().getId());
        } catch (Exception e) {
            logger.error("Error updating dues for new payment ID: {}",
                        event.getPayment().getId(), e);
            throw e;
        }
    }

    @EventListener
    @Async
    @Transactional
    public void handlePaymentDeactivated(PaymentDeactivatedEvent event) {
        logger.info("Handling payment deactivated event for payment ID: {}",
                   event.getPayment().getId());

        try {
            RecalculateDuesForDeactivatedPaymentCommand command =
                new RecalculateDuesForDeactivatedPaymentCommand(
                    event.getPayment(), feeDuesTrackerService, duesCalculationService);
            commandProcessor.process(command);
            logger.info("Successfully recalculated dues for deactivated payment ID: {}",
                       event.getPayment().getId());
        } catch (Exception e) {
            logger.error("Error recalculating dues for deactivated payment ID: {}",
                        event.getPayment().getId(), e);
            throw e;
        }
    }
}
