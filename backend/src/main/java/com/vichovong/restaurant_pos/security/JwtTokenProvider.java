package com.vichovong.restaurant_pos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long guestExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.guest-expiration-ms}") long guestExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.guestExpirationMs = guestExpirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getGuestExpirationMs() {
        return guestExpirationMs;
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // Short-lived token for QR-table guests; carries the session id instead of a
    // username plus a per-scan device id (one ordering turn per device)
    public String generateGuestToken(UUID sessionId, UUID deviceId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + guestExpirationMs);
        return Jwts.builder()
                .subject("guest")
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, SecurityConstants.TOKEN_TYPE_GUEST)
                .claim(SecurityConstants.CLAIM_SESSION_ID, sessionId.toString())
                .claim(SecurityConstants.CLAIM_DEVICE_ID, deviceId.toString())
                .claim("roles", List.of(SecurityConstants.ROLE_GUEST))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isGuestToken(String token) {
        return SecurityConstants.TOKEN_TYPE_GUEST
                .equals(parseClaims(token).get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class));
    }

    public UUID getSessionId(String token) {
        String sessionId = parseClaims(token).get(SecurityConstants.CLAIM_SESSION_ID, String.class);
        return sessionId == null ? null : UUID.fromString(sessionId);
    }

    public UUID getDeviceId(String token) {
        String deviceId = parseClaims(token).get(SecurityConstants.CLAIM_DEVICE_ID, String.class);
        return deviceId == null ? null : UUID.fromString(deviceId);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
