package com.smartdorm.backend.dto;

public record LoginResponse(
        String token,
        UserDto user
) {}