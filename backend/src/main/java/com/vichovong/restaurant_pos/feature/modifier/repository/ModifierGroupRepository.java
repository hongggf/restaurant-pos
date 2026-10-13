package com.vichovong.restaurant_pos.feature.modifier.repository;

import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {
}
