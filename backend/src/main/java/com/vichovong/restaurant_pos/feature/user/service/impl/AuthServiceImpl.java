package com.vichovong.restaurant_pos.feature.user.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.user.dto.LoginRequest;
import com.vichovong.restaurant_pos.feature.user.dto.LoginResponse;
import com.vichovong.restaurant_pos.feature.user.dto.UserCreateRequest;
import com.vichovong.restaurant_pos.feature.user.dto.UserResponse;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import com.vichovong.restaurant_pos.feature.user.repository.UserRepository;
import com.vichovong.restaurant_pos.feature.user.service.AuthService;
import com.vichovong.restaurant_pos.feature.user.service.UserService;
import com.vichovong.restaurant_pos.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        String token = jwtTokenProvider.generateToken(user.getUsername(), roles);
        return new LoginResponse(token, jwtTokenProvider.getExpirationMs());
    }

    @Override
    public UserResponse register(UserCreateRequest request) {
        return userService.create(request);
    }
}
