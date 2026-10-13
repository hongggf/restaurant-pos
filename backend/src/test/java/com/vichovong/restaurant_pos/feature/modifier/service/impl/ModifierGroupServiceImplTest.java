package com.vichovong.restaurant_pos.feature.modifier.service.impl;

import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupCreateRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupResponse;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupUpdateRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierOptionRequest;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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
class ModifierGroupServiceImplTest {

    @Mock
    private ModifierGroupRepository modifierGroupRepository;
    @Mock
    private MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    @Spy
    private ModifierMapper modifierMapper = new ModifierMapper() {};

    @InjectMocks
    private ModifierGroupServiceImpl modifierGroupService;

    private ModifierGroup group;
    private ModifierOption option1;
    private ModifierOption option2;

    @BeforeEach
    void setUp() {
        group = new ModifierGroup();
        group.setId(UUID.randomUUID());
        group.setNameEn("Sugar Level");
        group.setNameKm("កម្រិតជាតិស្ករ");
        group.setMinChoice(1);
        group.setMaxChoice(1);
        group.setActive(true);
        group.setCreatedAt(Instant.now());
        group.setUpdatedAt(Instant.now());

        option1 = new ModifierOption();
        option1.setId(UUID.randomUUID());
        option1.setModifierGroup(group);
        option1.setNameEn("100% Normal Sugar");
        option1.setNameKm("ស្ករធម្មតា ១០០%");
        option1.setUnitPrice(BigDecimal.ZERO);
        option1.setSortOrder(1);
        option1.setAvailable(true);

        option2 = new ModifierOption();
        option2.setId(UUID.randomUUID());
        option2.setModifierGroup(group);
        option2.setNameEn("50% Less Sugar");
        option2.setNameKm("ស្ករតិច ៥០%");
        option2.setUnitPrice(BigDecimal.ZERO);
        option2.setSortOrder(2);
        option2.setAvailable(true);

        group.setOptions(new ArrayList<>(List.of(option1, option2)));
    }

    @Test
    @DisplayName("getAll: returns paged modifier groups")
    void getAll_returnsPagedModifierGroups() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ModifierGroup> page = new PageImpl<>(List.of(group), pageable, 1);
        when(modifierGroupRepository.findAll(pageable)).thenReturn(page);

        PageResponse<ModifierGroupResponse> response = modifierGroupService.getAll(pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().nameEn()).isEqualTo("Sugar Level");
        assertThat(response.content().getFirst().options()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getById: returns modifier group when found")
    void getById_existingId_returnsGroupResponse() {
        when(modifierGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        ModifierGroupResponse response = modifierGroupService.getById(group.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(group.getId());
        assertThat(response.nameEn()).isEqualTo("Sugar Level");
        assertThat(response.minChoice()).isEqualTo(1);
        assertThat(response.maxChoice()).isEqualTo(1);
        assertThat(response.options()).hasSize(2);
    }

    @Test
    @DisplayName("getById: throws NOT_FOUND when group does not exist")
    void getById_nonExistingId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(modifierGroupRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modifierGroupService.getById(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create: successfully saves modifier group and its options")
    void create_validRequest_savesAndReturnsResponse() {
        ModifierOptionRequest optReq1 = new ModifierOptionRequest(
                null, "Oat Milk", "ទឹកដោះគោអូត", null, new BigDecimal("0.75"), null, true, 1
        );
        ModifierOptionRequest optReq2 = new ModifierOptionRequest(
                null, "Soy Milk", "ទឹកដោះគោសណ្តែក", null, new BigDecimal("0.50"), null, true, 2
        );

        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                "Milk Choice", "ជម្រើសទឹកដោះគោ", 0, 1, true, List.of(optReq1, optReq2)
        );

        when(modifierGroupRepository.save(any(ModifierGroup.class))).thenAnswer(invocation -> {
            ModifierGroup saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        ModifierGroupResponse response = modifierGroupService.create(request);

        assertThat(response.nameEn()).isEqualTo("Milk Choice");
        assertThat(response.minChoice()).isEqualTo(0);
        assertThat(response.maxChoice()).isEqualTo(1);
        assertThat(response.options()).hasSize(2);
        assertThat(response.options().get(0).nameEn()).isEqualTo("Oat Milk");
        assertThat(response.options().get(0).unitPrice()).isEqualByComparingTo("0.75");
    }

    @Test
    @DisplayName("create: throws BAD_REQUEST when maxChoice is less than minChoice")
    void create_invalidChoiceLimits_throwsBadRequest() {
        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                "Size", "ទំហំ", 2, 1, true, List.of()
        );

        assertThatThrownBy(() -> modifierGroupService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("update: successfully updates existing group, syncs options (add, update, delete, toggle availability)")
    void update_validRequest_syncsOptionsProperly() {
        // Update option1 (toggle availability to false), remove option2, add new option3 (null id)
        ModifierOptionRequest updateOpt1 = new ModifierOptionRequest(
                option1.getId(), "100% Normal Sugar", "ស្ករធម្មតា ១០០%", null, BigDecimal.ZERO, null, false, 1
        );
        ModifierOptionRequest newOpt3 = new ModifierOptionRequest(
                null, "0% No Sugar", "គ្មានស្ករ ០%", null, BigDecimal.ZERO, null, true, 3
        );

        ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                "Sugar Level (Updated)", "កម្រិតជាតិស្ករ", 1, 2, true, List.of(updateOpt1, newOpt3)
        );

        when(modifierGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        ModifierGroupResponse response = modifierGroupService.update(group.getId(), request);

        assertThat(response.nameEn()).isEqualTo("Sugar Level (Updated)");
        assertThat(response.maxChoice()).isEqualTo(2);
        assertThat(response.options()).hasSize(2);

        ModifierOption option1InGroup = group.getOptions().stream()
                .filter(o -> option1.getId().equals(o.getId()))
                .findFirst().orElseThrow();
        assertThat(option1InGroup.isAvailable()).isFalse(); // availability toggled

        // option2 was removed
        assertThat(group.getOptions().stream().anyMatch(o -> option2.getId().equals(o.getId()))).isFalse();

        // option3 was added
        assertThat(group.getOptions().stream().anyMatch(o -> "0% No Sugar".equals(o.getNameEn()))).isTrue();
    }

    @Test
    @DisplayName("update: throws BAD_REQUEST when request contains duplicate option IDs")
    void update_duplicateOptionIds_throwsBadRequest() {
        ModifierOptionRequest opt1 = new ModifierOptionRequest(
                option1.getId(), "Opt 1", "ជម្រើស ១", null, BigDecimal.ZERO, null, true, 1
        );
        ModifierOptionRequest opt1Duplicate = new ModifierOptionRequest(
                option1.getId(), "Opt 1 Dup", "ជម្រើស ១", null, BigDecimal.ZERO, null, true, 2
        );

        ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                "Sugar Level", "កម្រិតជាតិស្ករ", 0, 1, true, List.of(opt1, opt1Duplicate)
        );

        when(modifierGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> modifierGroupService.update(group.getId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("update: throws BAD_REQUEST when option ID does not belong to group")
    void update_foreignOptionId_throwsBadRequest() {
        UUID foreignOptionId = UUID.randomUUID();
        ModifierOptionRequest foreignOpt = new ModifierOptionRequest(
                foreignOptionId, "Foreign Option", "ជម្រើស", null, BigDecimal.ZERO, null, true, 1
        );

        ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                "Sugar Level", "កម្រិតជាតិស្ករ", 0, 1, true, List.of(foreignOpt)
        );

        when(modifierGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> modifierGroupService.update(group.getId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("update: throws NOT_FOUND when group does not exist")
    void update_nonExistingGroup_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                "Sugar Level", "កម្រិតជាតិស្ករ", 0, 1, true, List.of()
        );

        when(modifierGroupRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modifierGroupService.update(unknownId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("delete: deletes group when not attached to menu items")
    void delete_notAttachedToMenuItems_deletesSuccessfully() {
        when(modifierGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(menuItemModifierGroupRepository.existsByModifierGroupId(group.getId())).thenReturn(false);

        modifierGroupService.delete(group.getId());

        verify(modifierGroupRepository).delete(group);
    }

    @Test
    @DisplayName("delete: throws CONFLICT when group is attached to menu items")
    void delete_attachedToMenuItems_throwsConflict() {
        when(modifierGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(menuItemModifierGroupRepository.existsByModifierGroupId(group.getId())).thenReturn(true);

        assertThatThrownBy(() -> modifierGroupService.delete(group.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("delete: throws NOT_FOUND when group does not exist")
    void delete_nonExistingGroup_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(modifierGroupRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modifierGroupService.delete(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
