package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.DormDtos.*;
import com.smartdorm.backend.service.DormResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Secure all endpoints in this controller
public class DormResourceController {

    private final DormResourceService dormResourceService;

    public DormResourceController(DormResourceService dormResourceService) {
        this.dormResourceService = dormResourceService;
    }

    // --- Building Endpoints ---
    @GetMapping("/dorm-buildings")
    public ResponseEntity<List<DormBuildingDto>> getAllBuildings() {
        return ResponseEntity.ok(dormResourceService.getAllBuildings());
    }

    @PostMapping("/dorm-buildings")
    public ResponseEntity<DormBuildingDto> createBuilding(@Valid @RequestBody BuildingCreateUpdateDto dto) {
        return new ResponseEntity<>(dormResourceService.createBuilding(dto), HttpStatus.CREATED);
    }

    @PutMapping("/dorm-buildings/{buildingId}")
    public ResponseEntity<DormBuildingDto> updateBuilding(@PathVariable UUID buildingId, @Valid @RequestBody BuildingCreateUpdateDto dto) {
        return ResponseEntity.ok(dormResourceService.updateBuilding(buildingId, dto));
    }

    @DeleteMapping("/dorm-buildings/{buildingId}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable UUID buildingId) {
        dormResourceService.deleteBuilding(buildingId);
        return ResponseEntity.noContent().build();
    }

    // --- Room Endpoints ---
    @PostMapping("/dorm-rooms")
    public ResponseEntity<DormRoomDto> createRoom(@Valid @RequestBody RoomCreateUpdateDto dto) {
        return new ResponseEntity<>(dormResourceService.createRoom(dto), HttpStatus.CREATED);
    }

    @PutMapping("/dorm-rooms/{roomId}")
    public ResponseEntity<DormRoomDto> updateRoom(@PathVariable UUID roomId, @Valid @RequestBody RoomCreateUpdateDto dto) {
        return ResponseEntity.ok(dormResourceService.updateRoom(roomId, dto));
    }

    @DeleteMapping("/dorm-rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId) {
        dormResourceService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    // --- Bed Endpoints ---
    @PostMapping("/rooms/{roomId}/beds")
    public ResponseEntity<BedsCreatedResponseDto> createBeds(@PathVariable UUID roomId, @Valid @RequestBody BedCreateRequestDto dto) {
        BedsCreatedResponseDto response = dormResourceService.createBedsForRoom(roomId, dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/beds/{bedId}")
    public ResponseEntity<Void> deleteBed(@PathVariable UUID bedId) {
        dormResourceService.deleteBed(bedId);
        return ResponseEntity.noContent().build();
    }
}