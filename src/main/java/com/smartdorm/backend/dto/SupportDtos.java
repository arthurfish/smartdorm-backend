package com.smartdorm.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public class SupportDtos {

    // --- Feedback ---
    public record FeedbackCreateDto(
            boolean isAnonymous,
            @NotNull @Min(1) @Max(5) Integer rating,
            String comment
    ) {}

    // --- Swap Request ---
    public record SwapRequestCreateDto(
            @NotBlank String reason
    ) {}

    public record SwapRequestUpdateDto(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED", message = "Status must be APPROVED or REJECTED")
            String status,
            String adminComment
    ) {}

    public record SwapRequestDto(
            UUID id,
            UUID userId,
            String userName, // For admin convenience
            UUID cycleId,
            String reason,
            String status,
            String adminComment,
            Instant createdAt
    ) {}

    // --- Article ---
    public record ArticleCreateDto(
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String category
    ) {}

    public record ArticleUpdateDto(
            String title,
            String content,
            String category
    ) {}

    public record ArticleDto(
            UUID id,
            String title,
            String content,
            String category,
            UUID authorId,
            String authorName, // For convenience
            Instant createdAt
    ) {}

    // --- Notification ---
    public record NotificationDto(
            UUID id,
            String message,
            String linkUrl,
            boolean isRead,
            Instant createdAt
    ) {}
}