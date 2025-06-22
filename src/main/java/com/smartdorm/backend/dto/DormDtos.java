package com.smartdorm.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

// Using a single file for related, simple records can be convenient.

public class DormDtos {

    // --- Response DTOs ---

    public record DormBuildingDto(UUID id, String name) {}

    public record DormRoomDto(UUID id, UUID buildingId, String roomNumber, int capacity, String genderType) {}

    public record BedDto(UUID id, UUID roomId, int bedNumber) {}

    // --- Request DTOs ---

    public record BuildingCreateUpdateDto(@NotBlank String name) {}

    public record RoomCreateUpdateDto(
            @NotNull UUID buildingId,
            @NotBlank String roomNumber,
            @Min(1) int capacity,
            @NotBlank @Pattern(regexp = "MALE|FEMALE", message = "Gender must be MALE or FEMALE") String genderType
    ) {}

    public record BedCreateRequestDto(@NotNull @Min(1) Integer bedCount) {}

    public record BedsCreatedResponseDto(int count, List<BedDto> beds) {}
}