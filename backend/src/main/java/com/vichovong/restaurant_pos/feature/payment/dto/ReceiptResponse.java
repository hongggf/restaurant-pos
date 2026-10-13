package com.vichovong.restaurant_pos.feature.payment.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Full receipt payload returned by payment confirm (cashier spec §6) —
 * "confirm to print" is one call. receiptId/receiptNumber point at the stored
 * Phase 8 receipt; download the PDF at GET /api/v1/receipts/{receiptId}/pdf.
 */
public record ReceiptResponse(
        String restaurantName,
        UUID receiptId,
        String receiptNumber,
        Instant openedAt,
        Instant closedAt,
        BillResponse bill,
        PaymentResponse payment
) {
}
