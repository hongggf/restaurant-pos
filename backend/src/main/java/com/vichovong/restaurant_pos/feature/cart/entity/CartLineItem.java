package com.vichovong.restaurant_pos.feature.cart.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mutable draft line — lives only until the cart is sent as an order round,
 * at which point it is snapshotted and deleted (spec §B core invariant).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_line_items")
public class CartLineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    // Per-scan device that owns this draft line — drafts are private to a device
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 200)
    private String remark;

    @OneToMany(mappedBy = "cartLineItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartLineModifierSelection> selections = new ArrayList<>();
}
