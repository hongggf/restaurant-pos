package com.vichovong.restaurant_pos.feature.cart.service;

import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;

import java.util.List;

public interface CartPricingService {

    /**
     * Recomputes the full cart from current menu/modifier rows:
     * lineUnitPrice = item.price + sum(option.unitPrice x qty), VAT on the subtotal,
     * and USD->KHR dual display. Called after every cart read and mutation.
     */
    CartResponse price(TableSession session, List<CartLineItem> lines);
}
