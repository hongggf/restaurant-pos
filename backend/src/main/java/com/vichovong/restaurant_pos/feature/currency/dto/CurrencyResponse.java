package com.vichovong.restaurant_pos.feature.currency.dto;

import java.time.Instant;
import java.util.UUID;

public record CurrencyResponse(
        UUID id,
        String code,
        String name,
        String symbol,
        boolean defaultCurrency,
        Instant createdAt,
        Instant updatedAt
) {
}
