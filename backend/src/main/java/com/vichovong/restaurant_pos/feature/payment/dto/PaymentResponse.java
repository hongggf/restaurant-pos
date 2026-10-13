package com.vichovong.restaurant_pos.feature.payment.dto;

import com.vichovong.restaurant_pos.feature.payment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        PaymentMethod method,
        BigDecimal billTotal,
        BigDecimal amountTendered,
        String tenderedCurrency,
        BigDecimal changeUsd,
        BigDecimal changeKhr,
        String referenceNote,
        String paidBy,
        Instant paidAt
) {
}
