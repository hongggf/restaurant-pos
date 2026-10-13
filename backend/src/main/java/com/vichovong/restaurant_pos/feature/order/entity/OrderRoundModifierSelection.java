package com.vichovong.restaurant_pos.feature.order.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Snapshot modifier selection: option name and price delta copied at send-time.
 * modifierOption is an informational FK only.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_round_modifier_selections")
public class OrderRoundModifierSelection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_round_line_item_id", nullable = false)
    private OrderRoundLineItem orderRoundLineItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_option_id")
    private ModifierOption modifierOption;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_km", nullable = false, length = 150)
    private String nameKm;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;
}
