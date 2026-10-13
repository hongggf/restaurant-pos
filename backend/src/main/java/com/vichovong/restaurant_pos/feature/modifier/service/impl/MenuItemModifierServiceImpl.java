package com.vichovong.restaurant_pos.feature.modifier.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.menu.repository.MenuItemRepository;
import com.vichovong.restaurant_pos.feature.modifier.dto.AttachModifierGroupRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.AttachedModifierGroupResponse;
import com.vichovong.restaurant_pos.feature.modifier.entity.MenuItemModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.mapper.ModifierMapper;
import com.vichovong.restaurant_pos.feature.modifier.repository.MenuItemModifierGroupRepository;
import com.vichovong.restaurant_pos.feature.modifier.repository.ModifierGroupRepository;
import com.vichovong.restaurant_pos.feature.modifier.service.MenuItemModifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemModifierServiceImpl implements MenuItemModifierService {

    private final MenuItemRepository menuItemRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;
    private final ModifierMapper modifierMapper;

    @Override
    public List<AttachedModifierGroupResponse> getForMenuItem(UUID menuItemId) {
        requireMenuItem(menuItemId);
        return menuItemModifierGroupRepository.findByMenuItemIdOrderBySortOrderAsc(menuItemId).stream()
                .map(modifierMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AttachedModifierGroupResponse attach(UUID menuItemId, AttachModifierGroupRequest request) {
        MenuItem menuItem = requireMenuItem(menuItemId);
        ModifierGroup group = modifierGroupRepository.findById(request.modifierGroupId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Modifier group not found: " + request.modifierGroupId()));
        if (!group.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot attach inactive modifier group: " + group.getId());
        }
        if (menuItemModifierGroupRepository.existsByMenuItemIdAndModifierGroupId(menuItemId, group.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Modifier group already attached to this menu item: " + group.getId());
        }
        MenuItemModifierGroup attachment = new MenuItemModifierGroup();
        attachment.setMenuItem(menuItem);
        attachment.setModifierGroup(group);
        attachment.setSortOrder(request.sortOrder());
        return modifierMapper.toResponse(menuItemModifierGroupRepository.save(attachment));
    }

    @Override
    @Transactional
    public void detach(UUID menuItemId, UUID modifierGroupId) {
        MenuItemModifierGroup attachment = menuItemModifierGroupRepository
                .findByMenuItemIdAndModifierGroupId(menuItemId, modifierGroupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Modifier group not attached to this menu item: " + modifierGroupId));
        menuItemModifierGroupRepository.delete(attachment);
    }

    private MenuItem requireMenuItem(UUID menuItemId) {
        return menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Menu item not found: " + menuItemId));
    }
}
