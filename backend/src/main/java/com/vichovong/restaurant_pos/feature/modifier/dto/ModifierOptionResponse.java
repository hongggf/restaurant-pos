package com.vichovong.restaurant_pos.feature.modifier.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ModifierOptionResponse(
        UUID id,
        String nameEn,
        String nameKm,
        String imageUrl,
        BigDecimal unitPrice,
        String packSize,
        boolean available,
        int sortOrder
) {
}
