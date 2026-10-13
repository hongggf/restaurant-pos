package com.vichovong.restaurant_pos.feature.user.service;

import com.vichovong.restaurant_pos.feature.user.dto.LoginRequest;
import com.vichovong.restaurant_pos.feature.user.dto.LoginResponse;
import com.vichovong.restaurant_pos.feature.user.dto.UserCreateRequest;
import com.vichovong.restaurant_pos.feature.user.dto.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserResponse register(UserCreateRequest request);
}
