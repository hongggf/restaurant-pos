package com.vichovong.restaurant_pos.feature.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartLineResponse(
        UUID id,
        UUID menuItemId,
        String nameEn,
        String nameKm,
        String imageUrl,
        BigDecimal basePrice,
        int quantity,
        String remark,
        List<CartSelectionResponse> selections,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
