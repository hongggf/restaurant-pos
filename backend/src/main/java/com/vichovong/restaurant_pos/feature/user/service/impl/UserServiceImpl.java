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
import com.vichovong.restaurant_pos.feature.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRoundRepository orderRoundRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered: " + request.email());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRoles(resolveRoles(request.roles()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findUser(id));
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = findUser(id);
        if (!request.roles().contains(RoleName.ADMIN) || !request.enabled()) {
            ensureNotLastAdmin(user, "Cannot remove the admin role from or disable the last admin account");
        }
        user.setEmail(request.email());
        user.setEnabled(request.enabled());
        user.setRoles(resolveRoles(request.roles()));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = findUser(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && user.getUsername().equals(auth.getName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }
        ensureNotLastAdmin(user, "Cannot delete the last admin account");
        if (paymentRepository.existsByPaidById(id) || orderRoundRepository.existsByVoidedById(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "User has transaction history (payments or voided items) and cannot be deleted — deactivate the account instead");
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void resetPassword(UUID id, PasswordResetRequest request) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    private void ensureNotLastAdmin(User user, String message) {
        boolean isEnabledAdmin = user.isEnabled() && user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (isEnabledAdmin && userRepository.countByRolesNameAndEnabledTrue(RoleName.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, message);
        }
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        return roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown role: " + name)))
                .collect(Collectors.toSet());
    }
}
