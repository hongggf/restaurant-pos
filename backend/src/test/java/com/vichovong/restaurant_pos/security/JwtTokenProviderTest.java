package com.vichovong.restaurant_pos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "0123456789012345678901234567890123456789012345678901234567890123"; // 64 bytes
    private static final long STAFF_EXPIRY = 3600000; // 1 hr
    private static final long GUEST_EXPIRY = 1800000; // 30 mins

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, STAFF_EXPIRY, GUEST_EXPIRY);
    }

    @Test
    @DisplayName("generateToken: creates valid staff token with username and roles")
    void generateToken_staffToken_generatesAndValidates() {
        String token = jwtTokenProvider.generateToken("admin", List.of("ADMIN", "CASHIER"));

        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("admin");
        assertThat(jwtTokenProvider.isGuestToken(token)).isFalse();
        assertThat(jwtTokenProvider.getSessionId(token)).isNull();
    }

    @Test
    @DisplayName("generateGuestToken: creates valid guest token with session and device IDs")
    void generateGuestToken_guestToken_generatesAndExtractsClaims() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        String token = jwtTokenProvider.generateGuestToken(sessionId, deviceId);

        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("guest");
        assertThat(jwtTokenProvider.isGuestToken(token)).isTrue();
        assertThat(jwtTokenProvider.getSessionId(token)).isEqualTo(sessionId);
        assertThat(jwtTokenProvider.getDeviceId(token)).isEqualTo(deviceId);
    }

    @Test
    @DisplayName("validateToken: returns false for invalid or tampered token")
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.string")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(SECRET, -1000, -1000);
        String expiredToken = shortLivedProvider.generateToken("user", List.of("CASHIER"));

        assertThat(shortLivedProvider.validateToken(expiredToken)).isFalse();
    }
}
