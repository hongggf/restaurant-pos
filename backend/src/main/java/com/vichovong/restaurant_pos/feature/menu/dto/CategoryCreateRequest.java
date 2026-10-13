package com.vichovong.restaurant_pos.feature.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CategoryCreateRequest(
        @NotBlank String nameEn,
        @NotBlank String nameKm,
        String description,
        @NotNull @PositiveOrZero Integer sortOrder,
        boolean active,
        Boolean allowNotes
) {
}
