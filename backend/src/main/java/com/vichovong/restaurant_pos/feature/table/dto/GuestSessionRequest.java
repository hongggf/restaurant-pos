package com.vichovong.restaurant_pos.feature.table.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestSessionRequest(
        @NotBlank String qrToken
) {
}
