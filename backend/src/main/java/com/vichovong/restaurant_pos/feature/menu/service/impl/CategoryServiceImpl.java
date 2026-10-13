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
import com.vichovong.restaurant_pos.feature.menu.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResponse<CategoryResponse> getAll(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable).map(categoryMapper::toResponse));
    }

    @Override
    public List<CategoryResponse> getActive() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(UUID id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        Category category = new Category();
        applyRequest(category, request.nameEn(), request.nameKm(), request.description(),
                request.sortOrder(), request.active(), request.allowNotes());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
        Category category = findCategory(id);
        applyRequest(category, request.nameEn(), request.nameKm(), request.description(),
                request.sortOrder(), request.active(), request.allowNotes());
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = findCategory(id);
        if (menuItemRepository.existsByCategoryId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete category with existing menu items: " + id);
        }
        categoryRepository.delete(category);
    }

    private void applyRequest(Category category, String nameEn, String nameKm, String description,
                               int sortOrder, boolean active, Boolean allowNotes) {
        category.setNameEn(nameEn);
        category.setNameKm(nameKm);
        category.setDescription(description);
        category.setSortOrder(sortOrder);
        category.setActive(active);
        if (allowNotes != null) {
            category.setAllowNotes(allowNotes);
        }
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found: " + id));
    }
}
