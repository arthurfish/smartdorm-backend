package com.smartdorm.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CycleDtos {

    // --- Response DTOs ---
    public record MatchingCycleDto(UUID id, String name, Instant startDate, Instant endDate, String status) {}

    public record DimensionOptionDto(UUID id, String optionText, double optionValue) {}

    public record SurveyDimensionDto(
            UUID id,
            String dimensionKey,
            String prompt,
            String dimensionType,
            String responseType,
            double weight,
            String parentDimensionKey,
            boolean isReverseScored,
            List<DimensionOptionDto> options
    ) {}


    // --- Request/Create/Update DTOs ---
    public record MatchingCycleCreateDto(
            @NotEmpty String name,
            Instant startDate,
            Instant endDate
    ) {}

    public record MatchingCycleUpdateDto(
            String name,
            Instant startDate,
            Instant endDate,
            @Pattern(regexp = "DRAFT|OPEN|COMPLETED", message = "Status must be DRAFT, OPEN, or COMPLETED") String status
    ) {}

    public record OptionCreateDto(
            @NotEmpty String optionText,
            @NotNull Double optionValue
    ) {}

    public record SurveyDimensionCreateDto(
            @NotEmpty String dimensionKey,
            @NotEmpty String prompt,
            @NotEmpty @Pattern(regexp = "HARD_FILTER|SOFT_FACTOR", message = "dimensionType must be HARD_FILTER or SOFT_FACTOR")
            String dimensionType,
            @NotEmpty @Pattern(regexp = "SCALE|SINGLE_CHOICE|COMPOSITE", message = "responseType must be SCALE, SINGLE_CHOICE, or COMPOSITE")
            String responseType,
            @NotNull @PositiveOrZero Double weight,
            String parentDimensionKey,
            boolean isReverseScored,
            @Valid List<OptionCreateDto> options
    ) {}

    public record SurveyDimensionUpdateDto(
            @NotEmpty String prompt,
            @NotNull @PositiveOrZero Double weight,
            boolean isReverseScored
    ) {}
}