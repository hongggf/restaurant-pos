package com.vichovong.restaurant_pos.feature.table.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TableUpdateRequest(
        @NotBlank @Size(max = 20) String tableNumber,
        @NotNull Boolean active
) {
}
