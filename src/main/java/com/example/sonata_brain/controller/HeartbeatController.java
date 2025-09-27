package com.example.sonata_brain.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeartbeatController {
    /**
     * Maps HTTP GET requests to the /heartbeat endpoint.
     * When this endpoint is accessed, it returns the string "OK".
     * This is typically used for health checks to verify that the application is running.
     */
    @GetMapping("/heartbeat")
    public String heartbeat() {
        return "OK";
    }
}
