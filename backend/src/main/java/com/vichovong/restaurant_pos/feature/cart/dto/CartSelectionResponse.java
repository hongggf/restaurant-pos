package com.vichovong.restaurant_pos.feature.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartSelectionResponse(
        UUID modifierOptionId,
        String nameEn,
        String nameKm,
        BigDecimal unitPrice,
        int quantity
) {
}
