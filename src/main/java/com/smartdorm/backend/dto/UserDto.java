package com.smartdorm.backend.dto;

import java.util.UUID;

// DTO to safely expose user data, without the password
public record UserDto(
        UUID id,
        String studentId,
        String name,
        String role,
        String gender,
        String college
) {}