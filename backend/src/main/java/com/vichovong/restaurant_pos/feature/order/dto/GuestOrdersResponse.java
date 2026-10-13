package com.vichovong.restaurant_pos.feature.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * All sent rounds of the session plus the running grand total across
 * non-cancelled rounds — the cashier checkout hook (spec §B2).
 */
public record GuestOrdersResponse(
        UUID sessionId,
        List<OrderRoundResponse> rounds,
        String currencyCode,
        BigDecimal runningGrandTotal,
        BigDecimal runningGrandTotalKhr
) {
}
