package com.vichovong.restaurant_pos.feature.menu.dto;

import com.vichovong.restaurant_pos.feature.modifier.dto.AttachedModifierGroupResponse;

import java.util.List;

/**
 * Item detail for the guest ordering screen: the item plus its active modifier
 * groups (with options, including unavailable ones so the client can badge "N/A").
 */
public record GuestMenuItemDetailResponse(
        MenuItemResponse item,
        List<AttachedModifierGroupResponse> modifierGroups
) {
}
