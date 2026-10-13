package com.vichovong.restaurant_pos.feature.cart.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.cart.service.CartValidationService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.menu.repository.MenuItemRepository;
import com.vichovong.restaurant_pos.feature.modifier.entity.MenuItemModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.modifier.repository.MenuItemModifierGroupRepository;
import com.vichovong.restaurant_pos.feature.modifier.repository.ModifierOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartValidationServiceImpl implements CartValidationService {

    private final MenuItemRepository menuItemRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    @Override
    public MenuItem requireOrderableItem(UUID menuItemId) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Menu item not found: " + menuItemId));
        if (!item.isAvailable()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Menu item is not available: " + item.getNameEn());
        }
        return item;
    }

    /**
     * Choice counts are distinct options per group; option quantity covers
     * "2x" of the same add-on.
     */
    @Override
    public List<ModifierOption> validateSelections(MenuItem item, List<CartSelectionRequest> selections) {
        List<CartSelectionRequest> requested = selections == null ? List.of() : selections;

        Map<UUID, ModifierGroup> activeGroups = menuItemModifierGroupRepository
                .findByMenuItemIdOrderBySortOrderAsc(item.getId()).stream()
                .map(MenuItemModifierGroup::getModifierGroup)
                .filter(ModifierGroup::isActive)
                .collect(Collectors.toMap(ModifierGroup::getId, Function.identity()));

        Set<UUID> seenOptionIds = new HashSet<>();
        Map<UUID, Integer> choicesPerGroup = new HashMap<>();
        BigDecimal buildTotal = BigDecimal.ZERO;
        List<ModifierOption> resolved = new ArrayList<>(requested.size());

        for (CartSelectionRequest selection : requested) {
            if (!seenOptionIds.add(selection.modifierOptionId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Duplicate modifier option: " + selection.modifierOptionId());
            }
            if (selection.quantity() == null || selection.quantity() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Quantity must be at least 1 for modifier selection");
            }
            if (selection.quantity() > 20) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Maximum quantity per modifier option is 20");
            }
            ModifierOption option = modifierOptionRepository.findById(selection.modifierOptionId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "Unknown modifier option: " + selection.modifierOptionId()));
            ModifierGroup group = option.getModifierGroup();
            if (!activeGroups.containsKey(group.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Modifier option does not apply to this menu item: " + option.getNameEn());
            }
            if (!option.isAvailable()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Modifier option is not available: " + option.getNameEn());
            }
            if (group.getMaxChoice() != null && group.getMaxChoice() == 1 && selection.quantity() > 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "\"" + group.getNameEn() + "\" is single-choice and only allows quantity 1");
            }
            // Count portions, not ticked options: a bowl that holds 10 pieces of
            // meat is filled by 6 beef + 4 chicken just as much as by 10 different
            // meats. Counting selections instead let one option carry any quantity.
            choicesPerGroup.merge(group.getId(), selection.quantity(), Integer::sum);
            if (BuildMinimumPolicy.counts(group)) {
                buildTotal = buildTotal.add(option.getUnitPrice()
                        .multiply(BigDecimal.valueOf(selection.quantity())));
            }
            resolved.add(option);
        }

        for (ModifierGroup group : activeGroups.values()) {
            int chosen = choicesPerGroup.getOrDefault(group.getId(), 0);
            if (chosen < group.getMinChoice()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "\"" + group.getNameEn() + "\" requires at least " + group.getMinChoice() + " portion(s)");
            }
            if (group.getMaxChoice() != null && chosen > group.getMaxChoice()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "\"" + group.getNameEn() + "\" allows at most " + group.getMaxChoice() + " portion(s)");
            }
        }

        // A build has to carry real food, not just a free flavour. Only applies
        // to items that actually have the build groups attached.
        boolean isBuild = activeGroups.values().stream().anyMatch(BuildMinimumPolicy::counts);
        if (isBuild && buildTotal.compareTo(BuildMinimumPolicy.MINIMUM) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Choose at least $" + BuildMinimumPolicy.MINIMUM.toPlainString()
                            + " of meat, meatball, veggie, noodles or rice for this item");
        }
        return resolved;
    }
}
