package com.vichovong.restaurant_pos.feature.payment.service;

import com.vichovong.restaurant_pos.feature.payment.dto.BillResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.PaymentRequest;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;

import java.util.UUID;

/** Bill view and payment close-out for a table session (cashier spec §6). */
public interface SessionPaymentService {

    /** Works on ACTIVE (pre-payment) and CLOSED (reprint) sessions alike. */
    BillResponse getBill(UUID sessionId);

    /**
     * Confirms payment in one transaction: creates the Payment, completes all
     * non-cancelled rounds, closes the session (guests get 410 from then on),
     * and returns the full receipt payload.
     */
    ReceiptResponse pay(UUID sessionId, PaymentRequest request, String username);
}
