package com.vichovong.restaurant_pos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        rateLimitingFilter = new RateLimitingFilter(objectMapper);
    }

    @Test
    @DisplayName("RateLimiter: allows login requests up to limit (15) and blocks the 16th with 429")
    void loginEndpoint_rateLimitsAfter15Requests() throws ServletException, IOException {
        String clientIp = "192.168.1.50";

        // First 15 requests should pass through
        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        verify(filterChain, times(15)).doFilter(any(), any());

        // 16th request must be rejected with 429 Too Many Requests
        MockHttpServletRequest request16 = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request16.setRemoteAddr(clientIp);
        MockHttpServletResponse response16 = new MockHttpServletResponse();

        rateLimitingFilter.doFilterInternal(request16, response16, filterChain);

        assertThat(response16.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response16.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response16.getContentAsString()).contains("Too many login attempts");

        // Filter chain should still have only been called 15 times
        verify(filterChain, times(15)).doFilter(any(), any());
    }

    @Test
    @DisplayName("RateLimiter: allows guest session scan requests up to 60 and blocks 61st")
    void guestSessionEndpoint_rateLimitsAfter60Requests() throws ServletException, IOException {
        String clientIp = "192.168.1.60";

        for (int i = 1; i <= 60; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/guest/sessions");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        MockHttpServletRequest request61 = new MockHttpServletRequest("POST", "/api/v1/guest/sessions");
        request61.setRemoteAddr(clientIp);
        MockHttpServletResponse response61 = new MockHttpServletResponse();

        rateLimitingFilter.doFilterInternal(request61, response61, filterChain);

        assertThat(response61.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response61.getContentAsString()).contains("Too many QR scan requests");
    }

    @Test
    @DisplayName("RateLimiter: GET requests are not rate limited")
    void getRequests_notRateLimited() throws ServletException, IOException {
        for (int i = 1; i <= 20; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        verify(filterChain, times(20)).doFilter(any(), any());
    }

    @Test
    @DisplayName("RateLimiter: uses X-Forwarded-For header when present")
    void xForwardedForHeader_usedAsClientIp() throws ServletException, IOException {
        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        MockHttpServletRequest request16 = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request16.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");
        MockHttpServletResponse response16 = new MockHttpServletResponse();

        rateLimitingFilter.doFilterInternal(request16, response16, filterChain);
        assertThat(response16.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
