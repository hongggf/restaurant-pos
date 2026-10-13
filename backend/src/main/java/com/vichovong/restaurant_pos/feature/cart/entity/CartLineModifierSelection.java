package com.vichovong.restaurant_pos.feature.cart.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_line_modifier_selections",
        uniqueConstraints = @UniqueConstraint(name = "uq_cart_line_modifier_option",
                columnNames = {"cart_line_item_id", "modifier_option_id"}))
public class CartLineModifierSelection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_line_item_id", nullable = false)
    private CartLineItem cartLineItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modifier_option_id", nullable = false)
    private ModifierOption modifierOption;

    @Column(nullable = false)
    private int quantity;
}
