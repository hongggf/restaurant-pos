package com.vichovong.restaurant_pos.feature.user.service;

import com.vichovong.restaurant_pos.feature.user.dto.PasswordResetRequest;
import com.vichovong.restaurant_pos.feature.user.dto.UserCreateRequest;
import com.vichovong.restaurant_pos.feature.user.dto.UserResponse;
import com.vichovong.restaurant_pos.feature.user.dto.UserUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    UserResponse getById(UUID id);

    List<UserResponse> getAll();

    UserResponse update(UUID id, UserUpdateRequest request);

    void delete(UUID id);

    void resetPassword(UUID id, PasswordResetRequest request);
}
