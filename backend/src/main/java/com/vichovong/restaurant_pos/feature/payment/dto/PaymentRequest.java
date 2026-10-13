package com.vichovong.restaurant_pos.feature.payment.dto;

import com.vichovong.restaurant_pos.feature.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * CASH: amountTendered + currency (USD or KHR) required, change computed in both.
 * QR: no integration — optional free-text referenceNote from the cashier's manual
 * verification; tendered amount defaults to the bill total.
 */
public record PaymentRequest(
        @NotNull PaymentMethod method,
        @Positive BigDecimal amountTendered,
        String currency,
        @Size(max = 200) String referenceNote
) {
}
