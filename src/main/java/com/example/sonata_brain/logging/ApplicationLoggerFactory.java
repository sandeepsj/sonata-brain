package com.example.sonata_brain.logging;

import com.example.sonata_brain.logging.impl.ConsoleApplicationLogger;
import com.example.sonata_brain.logging.impl.KafkaApplicationLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApplicationLoggerFactory {

    // Singleton instance
    private static ApplicationLoggerFactory instance;
    private ApplicationLogger currentLogger;
    private Map<String, ApplicationLogger> loggerStrategies;
    private String currentStrategy;

    @Value("${application.logging.strategy:console}")
    private String defaultStrategy;

    // Private constructor for Singleton pattern
    private ApplicationLoggerFactory() {
        initializeLoggers();
    }

    // Thread-safe singleton implementation
    public static synchronized ApplicationLoggerFactory getInstance() {
        if (instance == null) {
            instance = new ApplicationLoggerFactory();
        }
        return instance;
    }

    // Initialize after Spring injection
    public void initialize() {
        if (currentStrategy == null) {
            setLoggingStrategy(defaultStrategy);
        }
    }

    private void initializeLoggers() {
        loggerStrategies = new HashMap<>();
        loggerStrategies.put("console", new ConsoleApplicationLogger());
        loggerStrategies.put("kafka", new KafkaApplicationLogger());
    }

    public synchronized void setLoggingStrategy(String strategy) {
        ApplicationLogger selectedLogger = loggerStrategies.get(strategy);

        if (selectedLogger != null) {
            this.currentLogger = selectedLogger;
            this.currentStrategy = strategy;
            System.out.println("[APPLICATION-LOGGER-SINGLETON] Switched to " + strategy + " logging strategy");
        } else {
            System.err.println("[APPLICATION-LOGGER-SINGLETON] Unknown logging strategy: " + strategy + ". Using console logger.");
            this.currentLogger = loggerStrategies.get("console");
            this.currentStrategy = "console";
        }
    }

    public String getCurrentStrategy() {
        return currentStrategy;
    }

    // Get the current logger instance for direct use
    public ApplicationLogger getLogger() {
        if (currentLogger == null) {
            setLoggingStrategy("console");
        }
        return currentLogger;
    }

    // Convenience methods for common logging operations
    public void logInfo(String category, String message) {
        getLogger().logInfo(category, message);
    }

    public void logError(String category, String message) {
        getLogger().logError(category, message);
    }

    public void logDebug(String category, String message) {
        getLogger().logDebug(category, message);
    }

    public void logWarn(String category, String message) {
        getLogger().logWarn(category, message);
    }

    public void logOperationStart(String operation, String entity, String details) {
        getLogger().logOperationStart(operation, entity, details);
    }

    public void logOperationSuccess(String operation, String entity, String details) {
        getLogger().logOperationSuccess(operation, entity, details);
    }

    public void logOperationFailure(String operation, String entity, String error) {
        getLogger().logOperationFailure(operation, entity, error);
    }

    public void logValidationStart(String entity, String validationType) {
        getLogger().logValidationStart(entity, validationType);
    }

    public void logValidationSuccess(String entity, String validationType) {
        getLogger().logValidationSuccess(entity, validationType);
    }

    public void logValidationFailure(String entity, String validationType, String error) {
        getLogger().logValidationFailure(entity, validationType, error);
    }

    public void logDatabaseOperation(String operation, String entity, String details) {
        getLogger().logDatabaseOperation(operation, entity, details);
    }

    public void logStrategySelection(String strategyType, String reason) {
        getLogger().logStrategySelection(strategyType, reason);
    }
}
