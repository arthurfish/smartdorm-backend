package com.smartdorm.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/api/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "message", "Service is running!");
    }
}