package com.vichovong.restaurant_pos.feature.menu.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemCreateRequest;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemResponse;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemUpdateRequest;
import com.vichovong.restaurant_pos.feature.menu.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponse>>> getAll(
            @PageableDefault(size = 20, sort = "nameEn") Pageable pageable,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean available) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getAll(pageable, categoryId, available)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> create(@Valid @RequestBody MenuItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created", menuItemService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody MenuItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Menu item updated", menuItemService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        menuItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted", null));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateAvailability(@PathVariable UUID id,
                                                                 @RequestParam boolean available) {
        return ResponseEntity.ok(ApiResponse.success("Availability updated", menuItemService.updateAvailability(id, available)));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> uploadImage(@PathVariable UUID id,
                                                                      @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", menuItemService.uploadImage(id, file)));
    }
}
