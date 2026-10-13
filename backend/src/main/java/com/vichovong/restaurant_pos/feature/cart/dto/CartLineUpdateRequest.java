package com.vichovong.restaurant_pos.feature.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Replaces quantity, remark, and the full selection set of an existing line
public record CartLineUpdateRequest(
        @NotNull @Min(1) Integer quantity,
        @Size(max = 200) String remark,
        @Valid List<CartSelectionRequest> selections
) {
}
