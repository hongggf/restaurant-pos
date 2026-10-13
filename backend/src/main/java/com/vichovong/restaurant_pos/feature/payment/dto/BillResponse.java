package com.vichovong.restaurant_pos.feature.payment.dto;

import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundResponse;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Pre-payment bill (cashier spec §6): non-cancelled rounds with non-voided lines,
 * computed at read time — there is no billing lock. Also serves as the reprint
 * view on a CLOSED session.
 */
public record BillResponse(
        UUID sessionId,
        String tableNumber,
        SessionStatus sessionStatus,
        List<OrderRoundResponse> rounds,
        String currencyCode,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal grandTotal,
        BigDecimal grandTotalKhr
) {
}
