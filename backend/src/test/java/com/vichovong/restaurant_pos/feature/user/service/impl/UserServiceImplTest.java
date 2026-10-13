package com.vichovong.restaurant_pos.feature.user.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.payment.repository.PaymentRepository;
import com.vichovong.restaurant_pos.feature.user.dto.PasswordResetRequest;
import com.vichovong.restaurant_pos.feature.user.dto.UserCreateRequest;
import com.vichovong.restaurant_pos.feature.user.dto.UserResponse;
import com.vichovong.restaurant_pos.feature.user.dto.UserUpdateRequest;
import com.vichovong.restaurant_pos.feature.user.entity.Role;
import com.vichovong.restaurant_pos.feature.user.entity.RoleName;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import com.vichovong.restaurant_pos.feature.user.mapper.UserMapper;
import com.vichovong.restaurant_pos.feature.user.repository.RoleRepository;
import com.vichovong.restaurant_pos.feature.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRoundRepository orderRoundRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserMapper userMapper = new UserMapper() {};

    @InjectMocks
    private UserServiceImpl userService;

    private Role adminRole;
    private Role cashierRole;
    private User adminUser;
    private User cashierUser;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName(RoleName.ADMIN);

        cashierRole = new Role();
        cashierRole.setId(UUID.randomUUID());
        cashierRole.setName(RoleName.CASHIER);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@restaurant.com");
        adminUser.setPassword("encoded-admin-pass");
        adminUser.setEnabled(true);
        adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
        adminUser.setCreatedAt(Instant.now());
        adminUser.setUpdatedAt(Instant.now());

        cashierUser = new User();
        cashierUser.setId(UUID.randomUUID());
        cashierUser.setUsername("cashier1");
        cashierUser.setEmail("cashier1@restaurant.com");
        cashierUser.setPassword("encoded-cashier-pass");
        cashierUser.setEnabled(true);
        cashierUser.setRoles(new HashSet<>(Set.of(cashierRole)));
        cashierUser.setCreatedAt(Instant.now());
        cashierUser.setUpdatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getAll: returns list of all users")
    void getAll_returnsUserList() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser, cashierUser));

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("admin");
        assertThat(result.get(1).username()).isEqualTo("cashier1");
    }

    @Test
    @DisplayName("getById: returns user when found")
    void getById_existingId_returnsUserResponse() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        UserResponse response = userService.getById(adminUser.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(adminUser.getId());
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.email()).isEqualTo("admin@restaurant.com");
        assertThat(response.roles()).containsExactly(RoleName.ADMIN);
    }

    @Test
    @DisplayName("getById: throws NOT_FOUND when user does not exist")
    void getById_nonExistingId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create: encodes password, assigns resolved roles, and saves user")
    void create_validRequest_hashesPasswordAndSaves() {
        UserCreateRequest request = new UserCreateRequest(
                "newuser",
                "newuser@restaurant.com",
                "Password@123",
                Set.of(RoleName.CASHIER)
        );

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@restaurant.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-pwd-xyz");
        when(roleRepository.findByName(RoleName.CASHIER)).thenReturn(Optional.of(cashierRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        UserResponse response = userService.create(request);

        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.email()).isEqualTo("newuser@restaurant.com");
        assertThat(response.enabled()).isTrue();
        assertThat(response.roles()).containsExactly(RoleName.CASHIER);
    }

    @Test
    @DisplayName("create: throws CONFLICT when username is already taken")
    void create_duplicateUsername_throwsConflict() {
        UserCreateRequest request = new UserCreateRequest(
                "admin",
                "newadmin@restaurant.com",
                "Password@123",
                Set.of(RoleName.ADMIN)
        );

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("create: throws CONFLICT when email is already registered")
    void create_duplicateEmail_throwsConflict() {
        UserCreateRequest request = new UserCreateRequest(
                "uniqueuser",
                "admin@restaurant.com",
                "Password@123",
                Set.of(RoleName.ADMIN)
        );

        when(userRepository.existsByUsername("uniqueuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@restaurant.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("create: throws BAD_REQUEST when requested role does not exist")
    void create_unknownRole_throwsBadRequest() {
        UserCreateRequest request = new UserCreateRequest(
                "newuser",
                "newuser@restaurant.com",
                "Password@123",
                Set.of(RoleName.CHEF)
        );

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@restaurant.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-pwd");
        when(roleRepository.findByName(RoleName.CHEF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("update: successfully updates user email, enabled state, and roles")
    void update_validRequest_updatesUser() {
        UserUpdateRequest request = new UserUpdateRequest(
                "cashier_updated@restaurant.com",
                false,
                Set.of(RoleName.CASHIER)
        );

        when(userRepository.findById(cashierUser.getId())).thenReturn(Optional.of(cashierUser));
        when(roleRepository.findByName(RoleName.CASHIER)).thenReturn(Optional.of(cashierRole));

        UserResponse response = userService.update(cashierUser.getId(), request);

        assertThat(response.email()).isEqualTo("cashier_updated@restaurant.com");
        assertThat(response.enabled()).isFalse();
        assertThat(response.roles()).containsExactly(RoleName.CASHIER);
    }

    @Test
    @DisplayName("update: throws CONFLICT when attempting to remove ADMIN role from the last active admin")
    void update_removeAdminRoleFromLastAdmin_throwsConflict() {
        UserUpdateRequest request = new UserUpdateRequest(
                "admin@restaurant.com",
                true,
                Set.of(RoleName.CASHIER) // Removing ADMIN role
        );

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRolesNameAndEnabledTrue(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.update(adminUser.getId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("update: throws CONFLICT when attempting to disable the last active admin")
    void update_disableLastAdmin_throwsConflict() {
        UserUpdateRequest request = new UserUpdateRequest(
                "admin@restaurant.com",
                false, // Disabling admin
                Set.of(RoleName.ADMIN)
        );

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRolesNameAndEnabledTrue(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.update(adminUser.getId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("update: allows demoting or disabling admin when other active admins exist")
    void update_demoteAdminWhenMultipleAdminsExist_succeeds() {
        UserUpdateRequest request = new UserUpdateRequest(
                "admin@restaurant.com",
                false,
                Set.of(RoleName.CASHIER)
        );

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRolesNameAndEnabledTrue(RoleName.ADMIN)).thenReturn(2L);
        when(roleRepository.findByName(RoleName.CASHIER)).thenReturn(Optional.of(cashierRole));

        UserResponse response = userService.update(adminUser.getId(), request);

        assertThat(response.enabled()).isFalse();
        assertThat(response.roles()).containsExactly(RoleName.CASHIER);
    }

    @Test
    @DisplayName("delete: deletes user when not self, not last admin, and no transaction history")
    void delete_validUser_deletesSuccessfully() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("other_user");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(cashierUser.getId())).thenReturn(Optional.of(cashierUser));
        when(paymentRepository.existsByPaidById(cashierUser.getId())).thenReturn(false);
        when(orderRoundRepository.existsByVoidedById(cashierUser.getId())).thenReturn(false);

        userService.delete(cashierUser.getId());

        verify(userRepository).delete(cashierUser);
    }

    @Test
    @DisplayName("delete: throws BAD_REQUEST when user tries to delete their own account")
    void delete_selfDeletion_throwsBadRequest() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.delete(adminUser.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("delete: throws CONFLICT when trying to delete the last active admin")
    void delete_lastAdmin_throwsConflict() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("super_root");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRolesNameAndEnabledTrue(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.delete(adminUser.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("delete: throws CONFLICT when user has payment history")
    void delete_userWithPaymentHistory_throwsConflict() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(cashierUser.getId())).thenReturn(Optional.of(cashierUser));
        when(paymentRepository.existsByPaidById(cashierUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> userService.delete(cashierUser.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("delete: throws CONFLICT when user has voided order round history")
    void delete_userWithVoidedRoundHistory_throwsConflict() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(cashierUser.getId())).thenReturn(Optional.of(cashierUser));
        when(paymentRepository.existsByPaidById(cashierUser.getId())).thenReturn(false);
        when(orderRoundRepository.existsByVoidedById(cashierUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> userService.delete(cashierUser.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("resetPassword: encodes and updates user password")
    void resetPassword_existingUser_updatesPassword() {
        PasswordResetRequest request = new PasswordResetRequest("NewSecurePassword@123");
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.encode("NewSecurePassword@123")).thenReturn("new-encoded-pass");

        userService.resetPassword(adminUser.getId(), request);

        assertThat(adminUser.getPassword()).isEqualTo("new-encoded-pass");
    }

    @Test
    @DisplayName("resetPassword: throws NOT_FOUND when user does not exist")
    void resetPassword_nonExistingUser_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        PasswordResetRequest request = new PasswordResetRequest("NewSecurePassword@123");
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(unknownId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
