package com.example.sonata_brain.logging;

public interface ApplicationLogger {
    // Generic logging methods
    void logInfo(String category, String message);
    void logError(String category, String message);
    void logDebug(String category, String message);
    void logWarn(String category, String message);
    void logTrace(String category, String message);

    // Business operation logging
    void logOperationStart(String operation, String entity, String details);
    void logOperationSuccess(String operation, String entity, String details);
    void logOperationFailure(String operation, String entity, String error);

    // Validation logging
    void logValidationStart(String entity, String validationType);
    void logValidationSuccess(String entity, String validationType);
    void logValidationFailure(String entity, String validationType, String error);

    // Database operation logging
    void logDatabaseOperation(String operation, String entity, String details);

    // Strategy/Decision logging
    void logStrategySelection(String strategyType, String reason);

    // Custom structured logging
    void logStructured(String level, String category, String operation, String entity, String message);
}
