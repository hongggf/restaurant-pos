package com.vichovong.restaurant_pos.feature.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

// No price fields by design — the server re-derives all prices from current menu rows
public record CartLineAddRequest(
        @NotNull UUID menuItemId,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 200) String remark,
        @Valid List<CartSelectionRequest> selections
) {
}
