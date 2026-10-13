package com.vichovong.restaurant_pos.feature.order.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable send-time snapshot of one cart send (spec §B core invariant).
 * Totals are copied at send-time and only ever recomputed by the cashier
 * void flow — never from live menu rows. Post-send rows are never hard-deleted;
 * cancelled rounds and voided lines remain as the audit trail.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_rounds",
        uniqueConstraints = @UniqueConstraint(name = "uq_order_rounds_session_round",
                columnNames = {"session_id", "round_number"}))
public class OrderRound extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    // Guest device that sent this round; null for cashier-submitted rounds.
    // A device with a round is "spent" — it can no longer draft or send.
    @Column(name = "device_id")
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false, length = 20)
    private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.NEW;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    // The FIFO cook queue is ordered by this (cashier spec §3)
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @OneToMany(mappedBy = "orderRound", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderRoundLineItem> lines = new ArrayList<>();
}
