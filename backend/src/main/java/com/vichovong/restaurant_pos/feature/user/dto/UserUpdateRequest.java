package com.vichovong.restaurant_pos.feature.user.dto;

import com.vichovong.restaurant_pos.feature.user.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UserUpdateRequest(
        @NotBlank @Email String email,
        @NotNull Boolean enabled,
        @NotEmpty Set<RoleName> roles
) {
}
