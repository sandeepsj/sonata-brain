package com.example.sonata_brain.service.dues.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DuesCommandProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DuesCommandProcessor.class);

    public void process(DuesTrackerCommand command) {
        logger.debug("Processing command: {}", command.getCommandType());

        try {
            command.execute();
            logger.debug("Successfully processed command: {}", command.getCommandType());
        } catch (Exception e) {
            logger.error("Error processing command: {}", command.getCommandType(), e);
            throw e;
        }
    }
}
