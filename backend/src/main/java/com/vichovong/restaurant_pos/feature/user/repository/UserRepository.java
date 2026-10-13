package com.vichovong.restaurant_pos.feature.user.repository;

import com.vichovong.restaurant_pos.feature.user.entity.RoleName;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    long countByRolesNameAndEnabledTrue(RoleName roleName);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
