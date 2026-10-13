package com.vichovong.restaurant_pos.feature.menu.mapper;

import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemResponse;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuItemMapper {

    default MenuItemResponse toResponse(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getNameEn(),
                menuItem.getNameKm(),
                menuItem.getDescriptionEn(),
                menuItem.getDescriptionKm(),
                menuItem.getPrice(),
                menuItem.getCurrency().getCode(),
                menuItem.getImageUrl(),
                menuItem.isAvailable(),
                menuItem.getCategory().getId(),
                menuItem.getCategory().getNameEn(),
                menuItem.getCategory() == null || menuItem.getCategory().isAllowNotes(),
                menuItem.getStation(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt()
        );
    }
}
