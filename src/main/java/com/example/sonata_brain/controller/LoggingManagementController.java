package com.example.sonata_brain.controller;

import com.example.sonata_brain.logging.ApplicationLoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logging")
@CrossOrigin(origins = "*")
public class LoggingManagementController {

    // Using generic ApplicationLoggerFactory instead of payment-specific one
    private final ApplicationLoggerFactory loggingFactory = ApplicationLoggerFactory.getInstance();

    @GetMapping("/strategy")
    public ResponseEntity<Map<String, String>> getCurrentStrategy() {
        Map<String, String> response = new HashMap<>();
        response.put("currentStrategy", loggingFactory.getCurrentStrategy());
        response.put("status", "active");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/strategy/{strategyName}")
    public ResponseEntity<Map<String, String>> switchLoggingStrategy(@PathVariable String strategyName) {
        try {
            loggingFactory.setLoggingStrategy(strategyName);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Logging strategy switched successfully");
            response.put("newStrategy", strategyName);
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to switch logging strategy");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", "error");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/strategies/available")
    public ResponseEntity<Map<String, Object>> getAvailableStrategies() {
        Map<String, Object> response = new HashMap<>();
        response.put("strategies", new String[]{"console", "kafka"});
        response.put("current", loggingFactory.getCurrentStrategy());
        response.put("description", Map.of(
            "console", "Logs to terminal/console output",
            "kafka", "Future implementation: Logs to Kafka topics"
        ));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testLogging() {
        // Test the current logging strategy with generic methods
        loggingFactory.logOperationStart("TEST_OPERATION", "TestEntity", "Testing generic logging system");
        loggingFactory.logValidationStart("TestEntity", "GENERIC_VALIDATION");
        loggingFactory.logDatabaseOperation("TEST_QUERY", "TestEntity", "Testing database logging");
        loggingFactory.logValidationSuccess("TestEntity", "GENERIC_VALIDATION");
        loggingFactory.logOperationSuccess("TEST_OPERATION", "TestEntity", "Test completed successfully");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Test logs generated successfully");
        response.put("strategy", loggingFactory.getCurrentStrategy());
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }
}
