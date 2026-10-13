package com.vichovong.restaurant_pos.feature.order.dto;

import com.vichovong.restaurant_pos.feature.cart.dto.CartLineAddRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Cashier manual ordering (cashier spec §5): same line shape and validation as
 * the guest cart, but submitted directly as a round — no draft cart staff-side.
 */
public record CashierRoundRequest(
        @NotEmpty @Valid List<CartLineAddRequest> lines
) {
}
