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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemModifierServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private ModifierGroupRepository modifierGroupRepository;
    @Mock
    private MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    @Spy
    private ModifierMapper modifierMapper = new ModifierMapper() {};

    @InjectMocks
    private MenuItemModifierServiceImpl menuItemModifierService;

    private MenuItem menuItem;
    private ModifierGroup modifierGroup;
    private MenuItemModifierGroup attachment;

    @BeforeEach
    void setUp() {
        menuItem = new MenuItem();
        menuItem.setId(UUID.randomUUID());
        menuItem.setNameEn("Latte");

        modifierGroup = new ModifierGroup();
        modifierGroup.setId(UUID.randomUUID());
        modifierGroup.setNameEn("Sugar Level");
        modifierGroup.setActive(true);
        modifierGroup.setOptions(new ArrayList<>());
        modifierGroup.setCreatedAt(Instant.now());
        modifierGroup.setUpdatedAt(Instant.now());

        attachment = new MenuItemModifierGroup();
        attachment.setId(UUID.randomUUID());
        attachment.setMenuItem(menuItem);
        attachment.setModifierGroup(modifierGroup);
        attachment.setSortOrder(1);
    }

    @Test
    @DisplayName("getForMenuItem: returns list of attached modifier groups for menu item")
    void getForMenuItem_existingMenuItem_returnsAttachedGroups() {
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(menuItemModifierGroupRepository.findByMenuItemIdOrderBySortOrderAsc(menuItem.getId()))
                .thenReturn(List.of(attachment));

        List<AttachedModifierGroupResponse> response = menuItemModifierService.getForMenuItem(menuItem.getId());

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().sortOrder()).isEqualTo(1);
        assertThat(response.getFirst().group().nameEn()).isEqualTo("Sugar Level");
    }

    @Test
    @DisplayName("getForMenuItem: throws NOT_FOUND when menu item does not exist")
    void getForMenuItem_nonExistingMenuItem_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(menuItemRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemModifierService.getForMenuItem(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("attach: successfully attaches active modifier group to menu item")
    void attach_validRequest_attachesGroup() {
        AttachModifierGroupRequest request = new AttachModifierGroupRequest(modifierGroup.getId(), 2);
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(modifierGroupRepository.findById(modifierGroup.getId())).thenReturn(Optional.of(modifierGroup));
        when(menuItemModifierGroupRepository.existsByMenuItemIdAndModifierGroupId(menuItem.getId(), modifierGroup.getId()))
                .thenReturn(false);
        when(menuItemModifierGroupRepository.save(any(MenuItemModifierGroup.class))).thenAnswer(invocation -> {
            MenuItemModifierGroup saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AttachedModifierGroupResponse response = menuItemModifierService.attach(menuItem.getId(), request);

        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.group().nameEn()).isEqualTo("Sugar Level");
    }

    @Test
    @DisplayName("attach: throws BAD_REQUEST when modifier group is inactive")
    void attach_inactiveGroup_throwsBadRequest() {
        modifierGroup.setActive(false);
        AttachModifierGroupRequest request = new AttachModifierGroupRequest(modifierGroup.getId(), 1);

        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(modifierGroupRepository.findById(modifierGroup.getId())).thenReturn(Optional.of(modifierGroup));

        assertThatThrownBy(() -> menuItemModifierService.attach(menuItem.getId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("attach: throws CONFLICT when modifier group is already attached")
    void attach_alreadyAttached_throwsConflict() {
        AttachModifierGroupRequest request = new AttachModifierGroupRequest(modifierGroup.getId(), 1);

        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(modifierGroupRepository.findById(modifierGroup.getId())).thenReturn(Optional.of(modifierGroup));
        when(menuItemModifierGroupRepository.existsByMenuItemIdAndModifierGroupId(menuItem.getId(), modifierGroup.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> menuItemModifierService.attach(menuItem.getId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("detach: detaches modifier group from menu item")
    void detach_existingAttachment_deletesAttachment() {
        when(menuItemModifierGroupRepository.findByMenuItemIdAndModifierGroupId(menuItem.getId(), modifierGroup.getId()))
                .thenReturn(Optional.of(attachment));

        menuItemModifierService.detach(menuItem.getId(), modifierGroup.getId());

        verify(menuItemModifierGroupRepository).delete(attachment);
    }

    @Test
    @DisplayName("detach: throws NOT_FOUND when modifier group is not attached")
    void detach_notAttached_throwsNotFound() {
        when(menuItemModifierGroupRepository.findByMenuItemIdAndModifierGroupId(menuItem.getId(), modifierGroup.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemModifierService.detach(menuItem.getId(), modifierGroup.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
