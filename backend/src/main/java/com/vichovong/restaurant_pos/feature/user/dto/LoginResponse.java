package com.vichovong.restaurant_pos.feature.user.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs
) {
    public LoginResponse(String accessToken, long expiresInMs) {
        this(accessToken, "Bearer", expiresInMs);
    }
}
