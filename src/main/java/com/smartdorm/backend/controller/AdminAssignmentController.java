package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.AdminDtos.AdminAssignmentValidationDto;
import com.smartdorm.backend.dto.AdminDtos.AssignmentResultAdminDto;
import com.smartdorm.backend.service.AdminAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    /**
     * [NEW] Endpoint to get the full list of assignment results for a cycle.
     * Corresponds to use case ADM-04.
     */
    @GetMapping("/results")
    public ResponseEntity<List<AssignmentResultAdminDto>> getAssignmentResults(@PathVariable UUID cycleId) {
        List<AssignmentResultAdminDto> results = adminAssignmentService.getAssignmentResults(cycleId);
        return ResponseEntity.ok(results);
    }

    /**
     * [ENHANCED] Endpoint to validate the quality of assignment results.
     * Corresponds to use case ADM-08.
     */
    @GetMapping("/validate-results")
    public ResponseEntity<AdminAssignmentValidationDto> validateResults(@PathVariable UUID cycleId) {
        AdminAssignmentValidationDto report = adminAssignmentService.validateResults(cycleId);
        return ResponseEntity.ok(report);
    }
}