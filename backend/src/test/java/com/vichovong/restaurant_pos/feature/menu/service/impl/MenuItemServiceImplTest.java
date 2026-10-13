package com.vichovong.restaurant_pos.feature.menu.service.impl;

import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.repository.CartLineItemRepository;
import com.vichovong.restaurant_pos.feature.currency.entity.Currency;
import com.vichovong.restaurant_pos.feature.currency.repository.CurrencyRepository;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemCreateRequest;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemResponse;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemUpdateRequest;
import com.vichovong.restaurant_pos.feature.menu.entity.Category;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.menu.mapper.MenuItemMapper;
import com.vichovong.restaurant_pos.feature.menu.repository.CategoryRepository;
import com.vichovong.restaurant_pos.feature.menu.repository.MenuItemRepository;
import com.vichovong.restaurant_pos.feature.modifier.repository.MenuItemModifierGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private MenuItemModifierGroupRepository menuItemModifierGroupRepository;
    @Mock
    private CartLineItemRepository cartLineItemRepository;

    @Spy
    private MenuItemMapper menuItemMapper = new MenuItemMapper() {};

    @InjectMocks
    private MenuItemServiceImpl menuItemService;

    @TempDir
    private Path tempDir;

    private Category category;
    private Currency currency;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(menuItemService, "uploadDir", tempDir.toString());

        category = new Category();
        category.setId(UUID.randomUUID());
        category.setNameEn("Beverages");
        category.setNameKm("ភេសជ្ជៈ");

        currency = new Currency();
        currency.setCode("USD");
        currency.setSymbol("$");

        menuItem = new MenuItem();
        menuItem.setId(UUID.randomUUID());
        menuItem.setNameEn("Iced Latte");
        menuItem.setNameKm("ឡាតេទឹកកក");
        menuItem.setDescriptionEn("Smooth espresso with milk");
        menuItem.setDescriptionKm("កាហ្វេទឹកដោះគោ");
        menuItem.setPrice(new BigDecimal("3.50"));
        menuItem.setCurrency(currency);
        menuItem.setAvailable(true);
        menuItem.setCategory(category);
        menuItem.setImageUrl("/uploads/latte.jpg");
        menuItem.setCreatedAt(Instant.now());
        menuItem.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("getAll: returns paged menu items without filters")
    void getAll_withoutFilters_returnsPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MenuItem> page = new PageImpl<>(List.of(menuItem), pageable, 1);
        when(menuItemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<MenuItemResponse> response = menuItemService.getAll(pageable, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().nameEn()).isEqualTo("Iced Latte");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAll: filters by categoryId and availability")
    void getAll_withCategoryAndAvailabilityFilters_returnsFilteredPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MenuItem> page = new PageImpl<>(List.of(menuItem), pageable, 1);
        when(menuItemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<MenuItemResponse> response = menuItemService.getAll(pageable, category.getId(), true);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().categoryId()).isEqualTo(category.getId());
        assertThat(response.content().getFirst().available()).isTrue();
    }

    @Test
    @DisplayName("getById: returns menu item when found")
    void getById_existingId_returnsMenuItemResponse() {
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));

        MenuItemResponse response = menuItemService.getById(menuItem.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(menuItem.getId());
        assertThat(response.nameEn()).isEqualTo("Iced Latte");
        assertThat(response.currencyCode()).isEqualTo("USD");
        assertThat(response.categoryNameEn()).isEqualTo("Beverages");
    }

    @Test
    @DisplayName("getById: throws NOT_FOUND when id does not exist")
    void getById_nonExistingId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(menuItemRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.getById(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create: successfully saves new menu item")
    void create_validRequest_savesAndReturnsResponse() {
        MenuItemCreateRequest request = new MenuItemCreateRequest(
                "Iced Latte",
                "ឡាតេទឹកកក",
                "Smooth espresso",
                "កាហ្វេ",
                new BigDecimal("3.50"),
                "USD",
                "https://example.com/latte.jpg",
                true,
                category.getId(),
                com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN
        );

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        MenuItemResponse response = menuItemService.create(request);

        assertThat(response.nameEn()).isEqualTo("Iced Latte");
        assertThat(response.price()).isEqualByComparingTo("3.50");
        assertThat(response.currencyCode()).isEqualTo("USD");
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.available()).isTrue();
    }

    @Test
    @DisplayName("create: throws BAD_REQUEST when category does not exist")
    void create_unknownCategory_throwsBadRequest() {
        MenuItemCreateRequest request = new MenuItemCreateRequest(
                "Iced Latte",
                "ឡាតេទឹកកក",
                null,
                null,
                new BigDecimal("3.50"),
                "USD",
                null,
                true,
                category.getId(),
                com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN
        );

        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("create: throws BAD_REQUEST when currency does not exist")
    void create_unknownCurrency_throwsBadRequest() {
        MenuItemCreateRequest request = new MenuItemCreateRequest(
                "Iced Latte",
                "ឡាតេទឹកកក",
                null,
                null,
                new BigDecimal("3.50"),
                "UNKNOWN",
                null,
                true,
                category.getId(),
                com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN
        );

        when(currencyRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("update: updates existing menu item fields and toggles availability")
    void update_validRequest_updatesAndReturnsResponse() {
        MenuItemUpdateRequest request = new MenuItemUpdateRequest(
                "Iced Mocha",
                "ម៉ូកាទឹកកក",
                "Rich chocolate & coffee",
                "សូកូឡា",
                new BigDecimal("4.00"),
                "USD",
                "https://example.com/mocha.jpg",
                false,
                category.getId(),
                com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN
        );

        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        MenuItemResponse response = menuItemService.update(menuItem.getId(), request);

        assertThat(response.nameEn()).isEqualTo("Iced Mocha");
        assertThat(response.price()).isEqualByComparingTo("4.00");
        assertThat(response.available()).isFalse();
        assertThat(response.imageUrl()).isEqualTo("https://example.com/mocha.jpg");
    }

    @Test
    @DisplayName("update: sets imageUrl to null when empty/blank string is provided")
    void update_blankImageUrl_setsImageUrlToNull() {
        MenuItemUpdateRequest request = new MenuItemUpdateRequest(
                "Hot Latte",
                "ឡាតេក្តៅ",
                "With steamed milk",
                "កាហ្វេក្តៅ",
                new BigDecimal("3.00"),
                "USD",
                "",
                false,
                category.getId(),
                com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN
        );

        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        MenuItemResponse response = menuItemService.update(menuItem.getId(), request);

        assertThat(response.imageUrl()).isNull();
    }

    @Test
    @DisplayName("delete: deletes menu item and removes cart line items when not attached to modifier groups")
    void delete_notAttachedToModifiers_deletesSuccessfully() {
        when(menuItemModifierGroupRepository.existsByMenuItemId(menuItem.getId())).thenReturn(false);
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));

        menuItemService.delete(menuItem.getId());

        verify(cartLineItemRepository).deleteByMenuItemId(menuItem.getId());
        verify(menuItemRepository).delete(menuItem);
    }

    @Test
    @DisplayName("delete: throws CONFLICT when menu item is attached to modifier groups")
    void delete_attachedToModifiers_throwsConflict() {
        when(menuItemModifierGroupRepository.existsByMenuItemId(menuItem.getId())).thenReturn(true);

        assertThatThrownBy(() -> menuItemService.delete(menuItem.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("uploadImage: uploads file, updates imageUrl, and returns response")
    void uploadImage_validFile_storesImageAndUpdatesUrl() {
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg==")
        );

        MenuItemResponse response = menuItemService.uploadImage(menuItem.getId(), file);

        assertThat(response.imageUrl()).contains("/uploads/menu-items/" + menuItem.getId());
        assertThat(response.imageUrl()).endsWith(".png");
    }

    @Test
    @DisplayName("uploadImage: throws BAD_REQUEST when uploaded file is empty")
    void uploadImage_emptyFile_throwsBadRequest() {
        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> menuItemService.uploadImage(menuItem.getId(), emptyFile))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
