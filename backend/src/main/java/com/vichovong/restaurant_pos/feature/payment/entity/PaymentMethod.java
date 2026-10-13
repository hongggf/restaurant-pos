package com.vichovong.restaurant_pos.feature.payment.entity;

/**
 * v1 methods (cashier spec §6): CASH with dual-currency change, and QR recorded
 * with manual verification only — no KHQR integration. CARD is deferred.
 */
public enum PaymentMethod {
    CASH,
    QR
}
