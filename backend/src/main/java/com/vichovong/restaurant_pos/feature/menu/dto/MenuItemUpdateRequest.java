package com.vichovong.restaurant_pos.feature.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemUpdateRequest(
        @NotBlank String nameEn,
        @NotBlank String nameKm,
        String descriptionEn,
        String descriptionKm,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotBlank String currencyCode,
        @Size(max = 500) String imageUrl,
        boolean available,
        @NotNull UUID categoryId,
        @NotNull com.vichovong.restaurant_pos.feature.menu.entity.StationType station
) {
}
