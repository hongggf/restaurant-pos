package com.vichovong.restaurant_pos.security;

public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String ROLE_PREFIX = "ROLE_";

    public static final String ROLE_GUEST = "GUEST";
    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String CLAIM_SESSION_ID = "sessionId";
    public static final String CLAIM_DEVICE_ID = "deviceId";
    public static final String TOKEN_TYPE_GUEST = "guest";
}
