package com.vichovong.restaurant_pos.feature.order.dto;

import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Replaces the full set of modifier selections on a sent line — same shape as ordering. */
public record UpdateLineSelectionsRequest(
        @NotNull @Valid List<CartSelectionRequest> selections
) {
}
