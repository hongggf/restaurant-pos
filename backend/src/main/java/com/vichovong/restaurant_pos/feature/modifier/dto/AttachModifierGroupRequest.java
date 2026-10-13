package com.vichovong.restaurant_pos.feature.modifier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record AttachModifierGroupRequest(
        @NotNull UUID modifierGroupId,
        @NotNull @PositiveOrZero Integer sortOrder
) {
}
