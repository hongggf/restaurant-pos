package com.vichovong.restaurant_pos.feature.menu.repository;

import com.vichovong.restaurant_pos.feature.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderBySortOrderAsc();
}
