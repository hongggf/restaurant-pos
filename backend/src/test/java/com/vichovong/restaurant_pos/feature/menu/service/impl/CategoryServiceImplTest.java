package com.vichovong.restaurant_pos.feature.menu.service.impl;

import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.menu.dto.CategoryCreateRequest;
import com.vichovong.restaurant_pos.feature.menu.dto.CategoryResponse;
import com.vichovong.restaurant_pos.feature.menu.dto.CategoryUpdateRequest;
import com.vichovong.restaurant_pos.feature.menu.entity.Category;
import com.vichovong.restaurant_pos.feature.menu.mapper.CategoryMapper;
import com.vichovong.restaurant_pos.feature.menu.repository.CategoryRepository;
import com.vichovong.restaurant_pos.feature.menu.repository.MenuItemRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MenuItemRepository menuItemRepository;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper() {};

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category1 = new Category();
        category1.setId(UUID.randomUUID());
        category1.setNameEn("Coffee");
        category1.setNameKm("កាហ្វេ");
        category1.setDescription("Hot and iced coffee drinks");
        category1.setSortOrder(1);
        category1.setActive(true);
        category1.setAllowNotes(true);
        category1.setCreatedAt(Instant.now());
        category1.setUpdatedAt(Instant.now());

        category2 = new Category();
        category2.setId(UUID.randomUUID());
        category2.setNameEn("Bakery");
        category2.setNameKm("នំបុ័ង");
        category2.setDescription("Freshly baked pastries");
        category2.setSortOrder(2);
        category2.setActive(false);
        category2.setAllowNotes(false);
        category2.setCreatedAt(Instant.now());
        category2.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("getAll: returns paged categories")
    void getAll_returnsPagedCategories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(category1, category2), pageable, 2);
        when(categoryRepository.findAll(pageable)).thenReturn(page);

        PageResponse<CategoryResponse> response = categoryService.getAll(pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).nameEn()).isEqualTo("Coffee");
        assertThat(response.content().get(1).nameEn()).isEqualTo("Bakery");
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("getActive: returns only active categories ordered by sort order")
    void getActive_returnsActiveCategoriesOrdered() {
        when(categoryRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(category1));

        List<CategoryResponse> response = categoryService.getActive();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().nameEn()).isEqualTo("Coffee");
        assertThat(response.getFirst().active()).isTrue();
    }

    @Test
    @DisplayName("getById: returns category when found")
    void getById_existingId_returnsCategoryResponse() {
        when(categoryRepository.findById(category1.getId())).thenReturn(Optional.of(category1));

        CategoryResponse response = categoryService.getById(category1.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(category1.getId());
        assertThat(response.nameEn()).isEqualTo("Coffee");
        assertThat(response.nameKm()).isEqualTo("កាហ្វេ");
        assertThat(response.sortOrder()).isEqualTo(1);
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("getById: throws NOT_FOUND when category does not exist")
    void getById_nonExistingId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(categoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create: saves new category and returns response")
    void create_validRequest_savesAndReturnsResponse() {
        CategoryCreateRequest request = new CategoryCreateRequest(
                "Tea",
                "តែ",
                "Loose leaf teas",
                3,
                true,
                true
        );

        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.nameEn()).isEqualTo("Tea");
        assertThat(response.nameKm()).isEqualTo("តែ");
        assertThat(response.description()).isEqualTo("Loose leaf teas");
        assertThat(response.sortOrder()).isEqualTo(3);
        assertThat(response.active()).isTrue();
        assertThat(response.allowNotes()).isTrue();
    }

    @Test
    @DisplayName("update: updates existing category fields")
    void update_validRequest_updatesAndReturnsResponse() {
        CategoryUpdateRequest request = new CategoryUpdateRequest(
                "Specialty Coffee",
                "កាហ្វេពិសេស",
                "Updated description",
                5,
                false,
                false
        );

        when(categoryRepository.findById(category1.getId())).thenReturn(Optional.of(category1));

        CategoryResponse response = categoryService.update(category1.getId(), request);

        assertThat(response.nameEn()).isEqualTo("Specialty Coffee");
        assertThat(response.nameKm()).isEqualTo("កាហ្វេពិសេស");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.sortOrder()).isEqualTo(5);
        assertThat(response.active()).isFalse();
        assertThat(response.allowNotes()).isFalse();
    }

    @Test
    @DisplayName("update: throws NOT_FOUND when category does not exist")
    void update_nonExistingId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        CategoryUpdateRequest request = new CategoryUpdateRequest(
                "Tea", "តែ", null, 1, true, true
        );

        when(categoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(unknownId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("delete: deletes category when no menu items exist")
    void delete_noMenuItems_deletesSuccessfully() {
        when(categoryRepository.findById(category1.getId())).thenReturn(Optional.of(category1));
        when(menuItemRepository.existsByCategoryId(category1.getId())).thenReturn(false);

        categoryService.delete(category1.getId());

        verify(categoryRepository).delete(category1);
    }

    @Test
    @DisplayName("delete: throws CONFLICT when menu items exist in category")
    void delete_withMenuItems_throwsConflict() {
        when(categoryRepository.findById(category1.getId())).thenReturn(Optional.of(category1));
        when(menuItemRepository.existsByCategoryId(category1.getId())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(category1.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("delete: throws NOT_FOUND when category does not exist")
    void delete_nonExistingId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(categoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
