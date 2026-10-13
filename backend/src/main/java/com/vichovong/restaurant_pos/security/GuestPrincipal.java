package com.vichovong.restaurant_pos.security;

import java.util.UUID;

/**
 * Authenticated principal for QR-table guests. Carries only the table-session id
 * and the per-scan device id — guests have no user account; both ids always come
 * from the token, never the URL.
 */
public record GuestPrincipal(UUID sessionId, UUID deviceId) {
}
