package com.smartdorm.backend.controller;

import com.smartdorm.backend.service.AdminAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/cycles/{cycleId}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssignmentController {

    private final AdminAssignmentService adminAssignmentService;

    public AdminAssignmentController(AdminAssignmentService adminAssignmentService) {
        this.adminAssignmentService = adminAssignmentService;
    }

    @PostMapping("/trigger-assignment")
    public ResponseEntity<Map<String, String>> triggerAssignment(@PathVariable UUID cycleId) {
        adminAssignmentService.triggerAssignment(cycleId);
        return ResponseEntity.accepted().body(Map.of("message", "Assignment process started."));
    }

    // The /results and /validate-results endpoints can be added here later.
}