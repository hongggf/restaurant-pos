package com.vichovong.restaurant_pos.feature.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full recomputed cart — returned by every cart read and mutation (spec §B2).
 * grandTotalKhr is the dual-display conversion; null when no USD→KHR rate is configured.
 */
public record CartResponse(
        UUID sessionId,
        List<CartLineResponse> lines,
        String currencyCode,
        BigDecimal subtotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal grandTotal,
        BigDecimal grandTotalKhr
) {
}
