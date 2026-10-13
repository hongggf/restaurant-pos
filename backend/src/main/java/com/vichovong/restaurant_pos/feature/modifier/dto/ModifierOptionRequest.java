package com.vichovong.restaurant_pos.feature.modifier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Nested option payload used inside group create/update.
 * On update: id present = update that option, id null = create a new one,
 * options missing from the list are deleted.
 */
public record ModifierOptionRequest(
        UUID id,
        @NotBlank String nameEn,
        @NotBlank String nameKm,
        @Size(max = 500) String imageUrl,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @Size(max = 50) String packSize,
        boolean available,
        @NotNull @PositiveOrZero Integer sortOrder
) {
}
