package com.vichovong.restaurant_pos.feature.order.dto;

import com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderRoundResponse(
        UUID id,
        int roundNumber,
        PaymentStatus paymentStatus,
        FulfillmentStatus fulfillmentStatus,
        BigDecimal subtotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal grandTotal,
        Instant sentAt,
        List<OrderRoundLineResponse> lines
) {
}
