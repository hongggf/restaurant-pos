package com.vichovong.restaurant_pos.feature.payment.service;

import com.vichovong.restaurant_pos.feature.payment.dto.BillResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.entity.Payment;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;

import java.math.BigDecimal;

/**
 * Assembles bill and receipt payloads from a session's rounds. Extracted from
 * SessionPaymentService so the receipt module can rebuild receipt data for
 * reprints without a circular dependency on the payment flow.
 */
public interface BillingService {

    BillResponse buildBill(TableSession session);

    /** Full receipt payload for a confirmed payment (pay response and PDF reprints). */
    ReceiptResponse buildReceiptPayload(Payment payment, java.util.UUID receiptId, String receiptNumber);

    /** USD → KHR at the configured rate, or null when no rate exists. */
    BigDecimal toKhrOrNull(BigDecimal usdAmount);
}
