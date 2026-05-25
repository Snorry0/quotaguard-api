package com.snor.quotaguard.dto.response;

import com.snor.quotaguard.domain.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
