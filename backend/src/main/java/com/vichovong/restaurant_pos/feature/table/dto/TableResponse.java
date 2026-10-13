package com.vichovong.restaurant_pos.feature.table.dto;

import java.time.Instant;
import java.util.UUID;

public record TableResponse(
        UUID id,
        String tableNumber,
        String qrToken,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
