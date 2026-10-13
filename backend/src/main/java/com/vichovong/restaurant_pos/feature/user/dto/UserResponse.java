package com.vichovong.restaurant_pos.feature.user.dto;

import com.vichovong.restaurant_pos.feature.user.entity.RoleName;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        Set<RoleName> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
