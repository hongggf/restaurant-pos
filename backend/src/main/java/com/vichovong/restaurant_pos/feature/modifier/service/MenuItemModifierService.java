package com.vichovong.restaurant_pos.feature.modifier.service;

import com.vichovong.restaurant_pos.feature.modifier.dto.AttachModifierGroupRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.AttachedModifierGroupResponse;

import java.util.List;
import java.util.UUID;

public interface MenuItemModifierService {

    List<AttachedModifierGroupResponse> getForMenuItem(UUID menuItemId);

    AttachedModifierGroupResponse attach(UUID menuItemId, AttachModifierGroupRequest request);

    void detach(UUID menuItemId, UUID modifierGroupId);
}
