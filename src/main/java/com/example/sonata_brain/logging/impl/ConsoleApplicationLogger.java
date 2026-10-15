package com.example.sonata_brain.logging.impl;

import com.example.sonata_brain.logging.ApplicationLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleApplicationLogger implements ApplicationLogger {

    // Logs through SLF4J (logger "app.<category>") so lines reach the console and the log table with the request
    // context (REQ-008.AC5).
    private void log(String level, String category, String message) {
        Logger logger = LoggerFactory.getLogger("app." + category);
        switch (level) {
            case "ERROR" -> logger.error(message);
            case "WARN" -> logger.warn(message);
            case "DEBUG" -> logger.debug(message);
            case "TRACE" -> logger.trace(message);
            default -> logger.info(message);
        }
    }

    @Override
    public void logInfo(String category, String message) {
        log("INFO", category, message);
    }

    @Override
    public void logError(String category, String message) {
        log("ERROR", category, message);
    }

    @Override
    public void logDebug(String category, String message) {
        log("DEBUG", category, message);
    }

    @Override
    public void logWarn(String category, String message) {
        log("WARN", category, message);
    }

    @Override
    public void logTrace(String category, String message) {
        log("TRACE", category, message);
    }

    @Override
    public void logOperationStart(String operation, String entity, String details) {
        logInfo("OPERATION", String.format("Starting %s for %s: %s", operation, entity, details));
    }

    @Override
    public void logOperationSuccess(String operation, String entity, String details) {
        logInfo("OPERATION", String.format("Completed %s for %s: %s", operation, entity, details));
    }

    @Override
    public void logOperationFailure(String operation, String entity, String error) {
        logError("OPERATION", String.format("Failed %s for %s: %s", operation, entity, error));
    }

    @Override
    public void logValidationStart(String entity, String validationType) {
        logDebug("VALIDATION", String.format("Starting %s validation for %s", validationType, entity));
    }

    @Override
    public void logValidationSuccess(String entity, String validationType) {
        logDebug("VALIDATION", String.format("Validation %s passed for %s", validationType, entity));
    }

    @Override
    public void logValidationFailure(String entity, String validationType, String error) {
        logError("VALIDATION", String.format("Validation %s failed for %s: %s", validationType, entity, error));
    }

    @Override
    public void logDatabaseOperation(String operation, String entity, String details) {
        logDebug("DATABASE", String.format("%s on %s: %s", operation, entity, details));
    }

    @Override
    public void logStrategySelection(String strategyType, String reason) {
        logDebug("STRATEGY", String.format("Selected %s strategy: %s", strategyType, reason));
    }

    @Override
    public void logStructured(String level, String category, String operation, String entity, String message) {
        String structuredMessage = String.format("Op:%s | Entity:%s | %s", operation, entity, message);
        switch (level.toUpperCase()) {
            case "INFO" -> logInfo(category, structuredMessage);
            case "ERROR" -> logError(category, structuredMessage);
            case "WARN" -> logWarn(category, structuredMessage);
            case "DEBUG" -> logDebug(category, structuredMessage);
            case "TRACE" -> logTrace(category, structuredMessage);
            default -> logInfo(category, structuredMessage);
        }
    }
}
