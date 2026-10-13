package com.vichovong.restaurant_pos.feature.user.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.user.dto.LoginRequest;
import com.vichovong.restaurant_pos.feature.user.dto.LoginResponse;
import com.vichovong.restaurant_pos.feature.user.entity.Role;
import com.vichovong.restaurant_pos.feature.user.entity.RoleName;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import com.vichovong.restaurant_pos.feature.user.repository.UserRepository;
import com.vichovong.restaurant_pos.feature.user.service.UserService;
import com.vichovong.restaurant_pos.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName(RoleName.ADMIN);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@restaurant.com");
        adminUser.setRoles(Set.of(adminRole));
    }

    @Test
    @DisplayName("login: returns JWT token on successful authentication")
    void login_validCredentials_returnsLoginResponse() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(jwtTokenProvider.generateToken("admin", List.of("ADMIN"))).thenReturn("jwt-token-123");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        LoginResponse response = authService.login(new LoginRequest("admin", "Admin@123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token-123");
        assertThat(response.expiresInMs()).isEqualTo(86400000L);
    }

    @Test
    @DisplayName("login: throws UNAUTHORIZED when authentication fails with BadCredentialsException")
    void login_badCredentials_throwsUnauthorized() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
