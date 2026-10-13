package com.vichovong.restaurant_pos.feature.currency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateCreateRequest(
        @NotBlank String fromCurrencyCode,
        @NotBlank String toCurrencyCode,
        @NotNull @Positive BigDecimal rate,
        @NotNull LocalDate effectiveDate
) {
}
