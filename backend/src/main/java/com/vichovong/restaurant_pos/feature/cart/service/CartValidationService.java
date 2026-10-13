package com.vichovong.restaurant_pos.feature.cart.service;

import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;

import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative validation against current menu/modifier rows (spec §B2).
 * Shared by cart mutations and round-send so both enforce identical rules.
 */
public interface CartValidationService {

    /** Loads a menu item and rejects missing (404) or unavailable (400) ones. */
    MenuItem requireOrderableItem(UUID menuItemId);

    /**
     * Validates the requested selections for the given item: rejects duplicate
     * options, foreign/unavailable options, and any attached active group whose
     * minChoice/maxChoice bounds are not met. Returns the resolved options in
     * the same order as the requests.
     */
    List<ModifierOption> validateSelections(MenuItem item, List<CartSelectionRequest> selections);
}
