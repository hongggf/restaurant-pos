package com.vichovong.restaurant_pos.feature.order.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot line: name and prices copied from the menu row at send-time.
 * menuItem is an informational FK only — display and pricing always use the
 * copied columns. Voiding sets the void fields and the cashier flow recomputes
 * the round totals from non-voided lines; the row itself is never deleted.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_round_line_items")
public class OrderRoundLineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_round_id", nullable = false)
    private OrderRound orderRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_km", nullable = false, length = 150)
    private String nameKm;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(length = 200)
    private String remark;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy;

    @Column(name = "void_reason", length = 200)
    private String voidReason;

    @OneToMany(mappedBy = "orderRoundLineItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderRoundModifierSelection> selections = new ArrayList<>();

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LineItemStatus status = LineItemStatus.SENT;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.vichovong.restaurant_pos.feature.menu.entity.StationType station = com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN;

    public boolean isVoided() {
        return voidedAt != null;
    }
}
