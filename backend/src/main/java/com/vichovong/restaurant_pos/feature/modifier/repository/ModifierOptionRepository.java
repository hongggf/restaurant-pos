package com.vichovong.restaurant_pos.feature.modifier.repository;

import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModifierOptionRepository extends JpaRepository<ModifierOption, UUID> {
}
