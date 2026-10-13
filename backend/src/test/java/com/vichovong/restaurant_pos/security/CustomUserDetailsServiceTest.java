package com.vichovong.restaurant_pos.security;

import com.vichovong.restaurant_pos.feature.user.entity.Role;
import com.vichovong.restaurant_pos.feature.user.entity.RoleName;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import com.vichovong.restaurant_pos.feature.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName(RoleName.ADMIN);

        Role cashierRole = new Role();
        cashierRole.setId(UUID.randomUUID());
        cashierRole.setName(RoleName.CASHIER);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("john_doe");
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setRoles(Set.of(adminRole, cashierRole));
    }

    @Test
    @DisplayName("loadUserByUsername: returns UserDetails with correct roles and credentials")
    void loadUserByUsername_existingUser_returnsUserDetails() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john_doe");

        assertThat(userDetails.getUsername()).isEqualTo("john_doe");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CASHIER");
    }

    @Test
    @DisplayName("loadUserByUsername: throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: unknown");
    }
}
