package com.vichovong.restaurant_pos.feature.modifier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record ModifierGroupCreateRequest(
        @NotBlank String nameEn,
        @NotBlank String nameKm,
        @NotNull @PositiveOrZero Integer minChoice,
        @Positive Integer maxChoice,
        boolean active,
        @NotNull List<@Valid ModifierOptionRequest> options
) {
}
