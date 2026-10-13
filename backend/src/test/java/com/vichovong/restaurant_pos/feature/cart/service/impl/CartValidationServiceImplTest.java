package com.vichovong.restaurant_pos.feature.cart.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.menu.repository.MenuItemRepository;
import com.vichovong.restaurant_pos.feature.modifier.entity.MenuItemModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.modifier.repository.MenuItemModifierGroupRepository;
import com.vichovong.restaurant_pos.feature.modifier.repository.ModifierOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartValidationServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private ModifierOptionRepository modifierOptionRepository;
    @Mock
    private MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    @InjectMocks
    private CartValidationServiceImpl cartValidationService;

    private MenuItem testItem;
    private ModifierGroup spiceGroup;
    private ModifierOption mildOption;
    private ModifierOption spicyOption;

    @BeforeEach
    void setUp() {
        testItem = new MenuItem();
        testItem.setId(UUID.randomUUID());
        testItem.setNameEn("Spicy Soup");
        testItem.setPrice(new BigDecimal("6.00"));
        testItem.setAvailable(true);

        spiceGroup = new ModifierGroup();
        spiceGroup.setId(UUID.randomUUID());
        spiceGroup.setNameEn("Spice Level");
        spiceGroup.setActive(true);
        spiceGroup.setMinChoice(1);
        spiceGroup.setMaxChoice(1);

        mildOption = new ModifierOption();
        mildOption.setId(UUID.randomUUID());
        mildOption.setNameEn("Mild");
        mildOption.setUnitPrice(BigDecimal.ZERO);
        mildOption.setAvailable(true);
        mildOption.setModifierGroup(spiceGroup);

        spicyOption = new ModifierOption();
        spicyOption.setId(UUID.randomUUID());
        spicyOption.setNameEn("Extra Spicy");
        spicyOption.setUnitPrice(new BigDecimal("0.50"));
        spicyOption.setAvailable(true);
        spicyOption.setModifierGroup(spiceGroup);
    }

    @Test
    @DisplayName("requireOrderableItem: throws NOT_FOUND when menu item does not exist")
    void requireOrderableItem_itemNotFound_throwsNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(menuItemRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartValidationService.requireOrderableItem(nonExistentId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("requireOrderableItem: throws BAD_REQUEST when menu item is not available")
    void requireOrderableItem_itemUnavailable_throwsBadRequest() {
        testItem.setAvailable(false);
        when(menuItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));

        assertThatThrownBy(() -> cartValidationService.requireOrderableItem(testItem.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("requireOrderableItem: returns item when available")
    void requireOrderableItem_itemAvailable_returnsItem() {
        when(menuItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));

        MenuItem result = cartValidationService.requireOrderableItem(testItem.getId());
        assertThat(result).isEqualTo(testItem);
    }

    @Test
    @DisplayName("validateSelections: throws BAD_REQUEST for duplicate modifier option selection")
    void validateSelections_duplicateOption_throwsBadRequest() {
        MenuItemModifierGroup link = new MenuItemModifierGroup();
        link.setModifierGroup(spiceGroup);
        when(menuItemModifierGroupRepository.findByMenuItemIdOrderBySortOrderAsc(testItem.getId()))
                .thenReturn(List.of(link));
        when(modifierOptionRepository.findById(mildOption.getId())).thenReturn(Optional.of(mildOption));

        List<CartSelectionRequest> duplicateSelections = List.of(
                new CartSelectionRequest(mildOption.getId(), 1),
                new CartSelectionRequest(mildOption.getId(), 1)
        );

        assertThatThrownBy(() -> cartValidationService.validateSelections(testItem, duplicateSelections))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Duplicate modifier option");
    }

    @Test
    @DisplayName("validateSelections: throws BAD_REQUEST when selection quantity is less than 1 or > 20")
    void validateSelections_invalidQuantity_throwsBadRequest() {
        MenuItemModifierGroup link = new MenuItemModifierGroup();
        link.setModifierGroup(spiceGroup);
        when(menuItemModifierGroupRepository.findByMenuItemIdOrderBySortOrderAsc(testItem.getId()))
                .thenReturn(List.of(link));

        List<CartSelectionRequest> zeroQty = List.of(new CartSelectionRequest(mildOption.getId(), 0));
        assertThatThrownBy(() -> cartValidationService.validateSelections(testItem, zeroQty))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Quantity must be at least 1");

        List<CartSelectionRequest> excessiveQty = List.of(new CartSelectionRequest(mildOption.getId(), 25));
        assertThatThrownBy(() -> cartValidationService.validateSelections(testItem, excessiveQty))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Maximum quantity per modifier option is 20");
    }

    @Test
    @DisplayName("validateSelections: throws BAD_REQUEST when single-choice group gets quantity > 1")
    void validateSelections_singleChoiceWithQuantityGreaterThanOne_throwsBadRequest() {
        MenuItemModifierGroup link = new MenuItemModifierGroup();
        link.setModifierGroup(spiceGroup);
        when(menuItemModifierGroupRepository.findByMenuItemIdOrderBySortOrderAsc(testItem.getId()))
                .thenReturn(List.of(link));
        when(modifierOptionRepository.findById(mildOption.getId())).thenReturn(Optional.of(mildOption));

        List<CartSelectionRequest> selections = List.of(new CartSelectionRequest(mildOption.getId(), 2));
        assertThatThrownBy(() -> cartValidationService.validateSelections(testItem, selections))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("is single-choice and only allows quantity 1");
    }

    @Test
    @DisplayName("validateSelections: throws BAD_REQUEST when minChoice is not met")
    void validateSelections_minChoiceNotMet_throwsBadRequest() {
        MenuItemModifierGroup link = new MenuItemModifierGroup();
        link.setModifierGroup(spiceGroup);
        when(menuItemModifierGroupRepository.findByMenuItemIdOrderBySortOrderAsc(testItem.getId()))
                .thenReturn(List.of(link));

        List<CartSelectionRequest> emptySelections = List.of();
        assertThatThrownBy(() -> cartValidationService.validateSelections(testItem, emptySelections))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("requires at least 1 portion(s)");
    }

    @Test
    @DisplayName("validateSelections: successfully validates valid selections and returns options")
    void validateSelections_validSelection_returnsResolvedOptions() {
        MenuItemModifierGroup link = new MenuItemModifierGroup();
        link.setModifierGroup(spiceGroup);
        when(menuItemModifierGroupRepository.findByMenuItemIdOrderBySortOrderAsc(testItem.getId()))
                .thenReturn(List.of(link));
        when(modifierOptionRepository.findById(mildOption.getId())).thenReturn(Optional.of(mildOption));

        List<CartSelectionRequest> validSelections = List.of(new CartSelectionRequest(mildOption.getId(), 1));
        List<ModifierOption> result = cartValidationService.validateSelections(testItem, validSelections);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mildOption.getId());
    }
}
