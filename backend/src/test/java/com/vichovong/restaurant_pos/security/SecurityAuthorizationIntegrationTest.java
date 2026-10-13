package com.vichovong.restaurant_pos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vichovong.restaurant_pos.config.SecurityConfig;
import com.vichovong.restaurant_pos.feature.cart.controller.GuestCartController;
import com.vichovong.restaurant_pos.feature.cart.service.GuestCartService;
import com.vichovong.restaurant_pos.feature.user.controller.UserController;
import com.vichovong.restaurant_pos.feature.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, GuestCartController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.guest-expiration-ms=14400000"
})
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UserService userService;

    @MockBean
    private GuestCartService guestCartService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/users is rejected (403/401)")
    void unauthenticated_usersEndpoint_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    @DisplayName("CASHIER role cannot access ADMIN-only /api/v1/users")
    void cashierRole_usersEndpoint_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    @DisplayName("GUEST role cannot access ADMIN-only /api/v1/users")
    void guestRole_usersEndpoint_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN role can access /api/v1/users")
    void adminRole_usersEndpoint_isAllowed() throws Exception {
        when(userService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/guest/cart is rejected")
    void unauthenticated_guestCartEndpoint_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/guest/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    @DisplayName("CASHIER role cannot access /api/v1/guest/cart (requires ROLE_GUEST)")
    void cashierRole_guestCartEndpoint_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/guest/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN role cannot access /api/v1/guest/cart (requires ROLE_GUEST)")
    void adminRole_guestCartEndpoint_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/guest/cart"))
                .andExpect(status().isForbidden());
    }
}
