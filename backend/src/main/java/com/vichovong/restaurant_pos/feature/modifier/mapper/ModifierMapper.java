package com.vichovong.restaurant_pos.feature.modifier.mapper;

import com.vichovong.restaurant_pos.feature.modifier.dto.AttachedModifierGroupResponse;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupResponse;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierOptionResponse;
import com.vichovong.restaurant_pos.feature.modifier.entity.MenuItemModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModifierMapper {

    default ModifierGroupResponse toResponse(ModifierGroup group) {
        if (group == null) {
            return null;
        }
        return new ModifierGroupResponse(
                group.getId(),
                group.getNameEn(),
                group.getNameKm(),
                group.getMinChoice(),
                group.getMaxChoice(),
                group.isActive(),
                group.getOptions().stream().map(this::toResponse).toList(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    default ModifierOptionResponse toResponse(ModifierOption option) {
        if (option == null) {
            return null;
        }
        return new ModifierOptionResponse(
                option.getId(),
                option.getNameEn(),
                option.getNameKm(),
                option.getImageUrl(),
                option.getUnitPrice(),
                option.getPackSize(),
                option.isAvailable(),
                option.getSortOrder()
        );
    }

    default AttachedModifierGroupResponse toResponse(MenuItemModifierGroup attachment) {
        if (attachment == null) {
            return null;
        }
        return new AttachedModifierGroupResponse(
                attachment.getSortOrder(),
                toResponse(attachment.getModifierGroup())
        );
    }
}
