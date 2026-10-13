package com.vichovong.restaurant_pos.feature.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Snapshot values as copied at send-time — never the live modifier row. */
public record OrderRoundSelectionResponse(
        UUID modifierOptionId,
        String nameEn,
        String nameKm,
        BigDecimal unitPrice,
        int quantity
) {
}
