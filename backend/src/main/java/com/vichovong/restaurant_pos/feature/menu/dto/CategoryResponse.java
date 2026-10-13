package com.vichovong.restaurant_pos.feature.menu.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String nameEn,
        String nameKm,
        String description,
        int sortOrder,
        boolean active,
        boolean allowNotes,
        Instant createdAt,
        Instant updatedAt
) {
}
