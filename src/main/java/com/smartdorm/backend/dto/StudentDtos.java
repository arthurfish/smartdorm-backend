package com.smartdorm.backend.dto;

import com.smartdorm.backend.dto.CycleDtos.SurveyDimensionDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class StudentDtos {

    // --- Response DTOs ---

    public record SurveyForStudentDto(UUID cycleId, List<SurveyDimensionDto> dimensions) {}

    public record AssignmentDetails(String building, String room, int bed) {}
    public record RoommateDto(String name, String studentId) {}
    public record AssignmentResultStudentDto(AssignmentDetails assignment, List<RoommateDto> roommates) {}


    // --- Request DTOs ---

    public record ResponseItem(
            @NotNull UUID dimensionId,
            @NotNull Double rawValue
    ) {}

    public record UserResponseSubmitDto(
            @NotNull @Valid List<ResponseItem> responses
    ) {}
}