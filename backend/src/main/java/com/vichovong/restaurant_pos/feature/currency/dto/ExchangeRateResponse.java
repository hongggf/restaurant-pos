package com.vichovong.restaurant_pos.feature.currency.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        String fromCurrencyCode,
        String toCurrencyCode,
        BigDecimal rate,
        LocalDate effectiveDate,
        Instant createdAt,
        Instant updatedAt
) {
}
