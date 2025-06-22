package com.smartdorm.backend.dto;

import java.util.List;
import java.util.UUID;

public class AdminDtos {

    /**
     * DTO for displaying assignment results in the admin panel.
     * Corresponds to ADM-04 use case.
     */
    public record AssignmentResultAdminDto(
            UserDto user,
            String building,
            String room,
            int bed
    ) {}

    /**
     * DTO for a single validation metric in the quality report.
     */
    public record ValidationDetailDto(
            String dorm,
            String metric,
            double value,
            boolean isCompliant
    ) {}

    /**
     * DTO for the overall assignment quality validation report.
     * Corresponds to ADM-08 use case.
     */
    public record AdminAssignmentValidationDto(
            boolean isValid,
            String message,
            List<ValidationDetailDto> details
    ) {}
}