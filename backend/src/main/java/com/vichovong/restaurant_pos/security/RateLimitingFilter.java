package com.vichovong.restaurant_pos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding window rate limiter for public sensitive endpoints:
 * - POST /api/v1/auth/login: max 15 requests/minute per IP (prevents brute-force)
 * - POST /api/v1/guest/sessions: max 60 requests/minute per IP (prevents QR token flooding)
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_LOGIN_REQUESTS_PER_MINUTE = 15;
    private static final int MAX_GUEST_SESSION_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_MS = 60_000L;

    private final ObjectMapper objectMapper;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    private record RequestCounter(AtomicInteger count, long windowStart) {}

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            if (path.endsWith("/api/v1/auth/login")) {
                if (isRateLimited("login:" + getClientIp(request), MAX_LOGIN_REQUESTS_PER_MINUTE)) {
                    sendRateLimitError(response, "Too many login attempts. Please wait a minute and try again.");
                    return;
                }
            } else if (path.endsWith("/api/v1/guest/sessions")) {
                if (isRateLimited("guest_session:" + getClientIp(request), MAX_GUEST_SESSION_REQUESTS_PER_MINUTE)) {
                    sendRateLimitError(response, "Too many QR scan requests. Please wait a minute and try again.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key, int maxRequests) {
        long now = System.currentTimeMillis();
        RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart() > WINDOW_MS) {
                return new RequestCounter(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        // Periodic cleanup when map grows
        if (requestCounts.size() > 5000) {
            requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart() > WINDOW_MS * 2);
        }

        return counter.count().get() > maxRequests;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
