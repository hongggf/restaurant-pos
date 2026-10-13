package com.vichovong.restaurant_pos.feature.payment.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment for a whole table session (cashier spec §6). Confirming it closes the
 * session and completes its rounds. billTotal is USD; the tendered amount is in
 * the currency the customer handed over, change is recorded in both currencies.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "bill_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal billTotal;

    @Column(name = "khr_grand_total", precision = 14, scale = 2)
    private BigDecimal khrGrandTotal;

    @Column(name = "amount_tendered", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountTendered;

    @Column(name = "tendered_currency", nullable = false, length = 3)
    private String tenderedCurrency;

    @Column(name = "change_usd", precision = 12, scale = 2)
    private BigDecimal changeUsd;

    @Column(name = "change_khr", precision = 14, scale = 2)
    private BigDecimal changeKhr;

    // QR: free-text note from the cashier's manual verification in their banking app
    @Column(name = "reference_note", length = 200)
    private String referenceNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;
}
