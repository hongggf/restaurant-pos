package com.vichovong.restaurant_pos.feature.menu.repository;

import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID>, JpaSpecificationExecutor<MenuItem> {

    boolean existsByCategoryId(UUID categoryId);
}
