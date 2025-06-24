package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.CycleDtos.*;
import com.smartdorm.backend.service.CycleManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/cycles")
@PreAuthorize("hasRole('ADMIN')")
public class CycleController {

    private final CycleManagementService cycleService;

    public CycleController(CycleManagementService cycleService) {
        this.cycleService = cycleService;
    }

    // --- Cycle Endpoints ---
    @PostMapping
    public ResponseEntity<MatchingCycleDto> createCycle(@Valid @RequestBody MatchingCycleCreateDto dto) {
        return new ResponseEntity<>(cycleService.createCycle(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MatchingCycleDto>> getAllCycles() {
        return ResponseEntity.ok(cycleService.getAllCycles());
    }

    @GetMapping("/{cycleId}")
    public ResponseEntity<MatchingCycleDto> getCycleById(@PathVariable UUID cycleId) {
        return ResponseEntity.ok(cycleService.getCycleById(cycleId));
    }

    @PutMapping("/{cycleId}")
    public ResponseEntity<MatchingCycleDto> updateCycle(@PathVariable UUID cycleId, @Valid @RequestBody MatchingCycleUpdateDto dto) {
        return ResponseEntity.ok(cycleService.updateCycle(cycleId, dto));
    }

    @DeleteMapping("/{cycleId}")
    public ResponseEntity<Void> deleteCycle(@PathVariable UUID cycleId) {
        cycleService.deleteCycle(cycleId);
        return ResponseEntity.noContent().build();
    }

    // --- Dimension Endpoints ---
    @PostMapping("/{cycleId}/dimensions")
    public ResponseEntity<SurveyDimensionDto> createDimension(@PathVariable UUID cycleId, @Valid @RequestBody SurveyDimensionCreateDto dto) {
        return new ResponseEntity<>(cycleService.createDimensionForCycle(cycleId, dto), HttpStatus.CREATED);
    }

    @GetMapping("/{cycleId}/dimensions")
    public ResponseEntity<List<SurveyDimensionDto>> getDimensions(@PathVariable UUID cycleId) {
        return ResponseEntity.ok(cycleService.getDimensionsForCycle(cycleId));
    }

    @PutMapping("/{cycleId}/dimensions/{dimensionId}")
    public ResponseEntity<SurveyDimensionDto> updateDimension(@PathVariable UUID cycleId, @PathVariable UUID dimensionId, @Valid @RequestBody SurveyDimensionUpdateDto dto) {
        // cycleId is not strictly needed for the update logic but good for RESTful path structure
        return ResponseEntity.ok(cycleService.updateDimension(dimensionId, dto));
    }

    @DeleteMapping("/{cycleId}/dimensions/{dimensionId}")
    public ResponseEntity<Void> deleteDimension(@PathVariable UUID cycleId, @PathVariable UUID dimensionId) {
        cycleService.deleteDimension(dimensionId);
        return ResponseEntity.noContent().build();
    }
}