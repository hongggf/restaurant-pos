package com.vichovong.restaurant_pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal: authenticates staff user with UserDetails and roles")
    void doFilter_validStaffToken_authenticatesUser() throws ServletException, IOException {
        String token = "valid-staff-jwt-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.isGuestToken(token)).thenReturn(false);
        when(jwtTokenProvider.getUsername(token)).thenReturn("admin");

        UserDetails userDetails = new User("admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .contains("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: authenticates guest with GuestPrincipal and ROLE_GUEST")
    void doFilter_validGuestToken_authenticatesGuest() throws ServletException, IOException {
        String token = "valid-guest-jwt-token";
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.isGuestToken(token)).thenReturn(true);
        when(jwtTokenProvider.getSessionId(token)).thenReturn(sessionId);
        when(jwtTokenProvider.getDeviceId(token)).thenReturn(deviceId);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(GuestPrincipal.class);
        GuestPrincipal principal = (GuestPrincipal) auth.getPrincipal();
        assertThat(principal.sessionId()).isEqualTo(sessionId);
        assertThat(principal.deviceId()).isEqualTo(deviceId);
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .contains("ROLE_GUEST");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: guest token without deviceId is rejected")
    void doFilter_guestTokenWithoutDeviceId_notAuthenticated() throws ServletException, IOException {
        String token = "guest-token-no-device";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.isGuestToken(token)).thenReturn(true);
        when(jwtTokenProvider.getDeviceId(token)).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: request without Authorization header leaves SecurityContext unauthenticated")
    void doFilter_noToken_leavesContextUnauthenticated() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
