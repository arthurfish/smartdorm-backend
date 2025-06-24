// src/main/java/com/smartdorm/backend/dto/CycleDtos.java
package com.smartdorm.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
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
            boolean reverseScored, // [修改] 字段名统一
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor // 为方便测试用例，也添加一个全参构造函数
    public static class OptionCreateDto {
        private String optionText;
        private Double optionValue;
    }

    // [关键重构] 将 SurveyDimensionCreateDto 从 record 改为 class
    @Data
    @NoArgsConstructor
    public static class SurveyDimensionCreateDto {
        @NotEmpty
        private String dimensionKey;

        @NotEmpty
        private String prompt;

        @NotEmpty @Pattern(regexp = "HARD_FILTER|SOFT_FACTOR")
        private String dimensionType = "SOFT_FACTOR";

        @NotEmpty @Pattern(regexp = "SCALE|SINGLE_CHOICE|COMPOSITE")
        private String responseType = "SINGLE_CHOICE";

        @NotNull @PositiveOrZero
        private Double weight = 1.0;

        private String parentDimensionKey;

        // [修改] 字段名统一为 reverseScored
        private boolean reverseScored = false;

        @Valid
        private List<OptionCreateDto> options = new ArrayList<>();

    }

    public record SurveyDimensionUpdateDto(
            @NotEmpty String prompt,
            @NotNull @PositiveOrZero Double weight,
            boolean reverseScored // [修改] 字段名统一
    ) {}
}