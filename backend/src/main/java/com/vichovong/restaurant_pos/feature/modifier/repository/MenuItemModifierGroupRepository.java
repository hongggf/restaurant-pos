package com.vichovong.restaurant_pos.feature.modifier.repository;

import com.vichovong.restaurant_pos.feature.modifier.entity.MenuItemModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemModifierGroupRepository extends JpaRepository<MenuItemModifierGroup, UUID> {

    List<MenuItemModifierGroup> findByMenuItemIdOrderBySortOrderAsc(UUID menuItemId);

    Optional<MenuItemModifierGroup> findByMenuItemIdAndModifierGroupId(UUID menuItemId, UUID modifierGroupId);

    boolean existsByMenuItemIdAndModifierGroupId(UUID menuItemId, UUID modifierGroupId);

    boolean existsByMenuItemId(UUID menuItemId);

    boolean existsByModifierGroupId(UUID modifierGroupId);
}
